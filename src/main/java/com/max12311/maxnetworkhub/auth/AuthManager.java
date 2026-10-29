package com.max12311.maxnetworkhub.auth;

import com.max12311.maxnetworkhub.MaxNetworkHub;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class AuthManager {
    private final MaxNetworkHub plugin;
    private final Map<UUID, AuthState> authStates = new HashMap<>();
    private final File dataFolder;

    // .dat file format: version byte (1=legacy, 2=sha256), then UTF hash, then UTF name
    private static final byte VERSION_LEGACY = 1;
    private static final byte VERSION_SHA256 = 2;

    public AuthManager(MaxNetworkHub plugin) {
        this(plugin, plugin.getDataFolder());
    }

    AuthManager(MaxNetworkHub plugin, File baseFolder) {
        this.plugin = plugin;
        this.dataFolder = new File(baseFolder, "playerdata");
        this.dataFolder.mkdirs();
    }

    public void startAuthFlow(Player player) {
        startAuthFlow(player, false);
    }

    public void startAuthFlow(Player player, boolean bedrock) {
        File playerFile = new File(dataFolder, player.getUniqueId() + ".dat");

        if (playerFile.exists()) {
            authStates.put(player.getUniqueId(), new AuthState(AuthState.Type.LOGIN, 3));
            if (bedrock) {
                player.sendMessage("§b[Bedrock] §ePlease log in. §fType your 4-digit PIN in chat:");
            } else {
                player.sendMessage("§ePlease log in. Enter your PIN:");
                plugin.getGuiManager().openPinVerify(player, pin -> verifyLogin(player, pin));
            }
        } else {
            authStates.put(player.getUniqueId(), new AuthState(AuthState.Type.REGISTER, 0));
            if (bedrock) {
                player.sendMessage("§b[Bedrock] §eWelcome! §fType a 4-digit PIN in chat to register:");
            } else {
                player.sendMessage("§eWelcome! Please create a 4-digit PIN:");
                plugin.getGuiManager().openPinRegister(player, pin -> completeRegister(player, pin));
            }
        }
    }

    public boolean isRegistered(UUID uuid) {
        return new File(dataFolder, uuid + ".dat").exists();
    }

    public boolean verifyPin(UUID uuid, String pin) {
        File f = new File(dataFolder, uuid + ".dat");
        if (!f.exists()) return false;
        try {
            byte[] raw = java.nio.file.Files.readAllBytes(f.toPath());
            // Version byte 1 or 2 is always < 3; a DataOutputStream-written UTF string starts
            // with a 2-byte length (high byte first). Legacy files start with 0x00 (high byte of
            // short length), so version byte 0 means "no version prefix" — treat as legacy.
            byte firstByte = raw[0];
            if (firstByte == VERSION_SHA256) {
                try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
                    in.readByte(); // version
                    String storedHash = in.readUTF();
                    return hashPinSha256(pin).equals(storedHash);
                }
            } else {
                // Legacy file: either version byte == 1, or old unversioned format (first byte is
                // the high byte of a UTF length, i.e. 0). Read UTF directly.
                String storedHash;
                String name = "";
                if (firstByte == VERSION_LEGACY) {
                    try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
                        in.readByte(); // version
                        storedHash = in.readUTF();
                        name = in.readUTF();
                    }
                } else {
                    // Truly old format: no version byte — read as UTF directly
                    try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
                        storedHash = in.readUTF();
                        try { name = in.readUTF(); } catch (IOException ignored) {}
                    }
                }
                if (hashPinLegacy(pin).equals(storedHash)) {
                    savePinFile(f, uuid, name, pin);
                    return true;
                }
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isAuthenticating(UUID uuid) {
        return authStates.containsKey(uuid);
    }

    public void submitPin(Player player, String pin) {
        UUID uuid = player.getUniqueId();
        AuthState state = authStates.get(uuid);
        if (state == null) return;

        if (state.type == AuthState.Type.LOGIN) {
            verifyLogin(player, pin);
        } else if (state.type == AuthState.Type.REGISTER) {
            completeRegister(player, pin);
        }
    }

    public String getAuthType(UUID uuid) {
        AuthState state = authStates.get(uuid);
        return state == null ? null : state.type.name();
    }

    public void startAdminAuth(Player player, Consumer<String> callback) {
        plugin.getGuiManager().openPinVerify(player, pin -> callback.accept(pin));
    }

    private void verifyLogin(Player player, String pin) {
        UUID uuid = player.getUniqueId();
        AuthState state = authStates.get(uuid);

        if (state == null || state.type != AuthState.Type.LOGIN) return;

        if (verifyPin(uuid, pin)) {
            player.sendMessage("§aLogin successful!");
            authStates.remove(uuid);
            // Sync PIN to proxy SQLite so admin verify and proxy checks stay synchronized
            plugin.getMessenger().sendSyncPin(player, pin);
            plugin.getMessenger().sendAuthSuccess(player);
        } else {
            state.attemptsLeft--;
            if (state.attemptsLeft <= 0) {
                player.sendMessage("§cToo many failed attempts. Kicking...");
                player.kickPlayer("§cToo many failed login attempts.\n§ePlease try again later.");
                authStates.remove(uuid);
            } else {
                player.sendMessage("§cWrong PIN. " + state.attemptsLeft + " attempts left.");
                plugin.getGuiManager().openPinVerify(player, p -> verifyLogin(player, p));
            }
        }
    }

    private void completeRegister(Player player, String pin) {
        UUID uuid = player.getUniqueId();
        AuthState state = authStates.get(uuid);

        if (state == null || state.type != AuthState.Type.REGISTER) return;

        File f = new File(dataFolder, uuid + ".dat");
        try {
            savePinFile(f, uuid, player.getName(), pin);
            player.sendMessage("§aRegistration complete! Your PIN is set.");
            authStates.remove(uuid);
            // Sync PIN to proxy SQLite so admin verify and VerifyPinGUI paths work
            plugin.getMessenger().sendSyncPin(player, pin);
            plugin.getMessenger().sendAuthSuccess(player);
        } catch (IOException e) {
            player.sendMessage("§cFailed to save your data. Please contact admin.");
        }
    }

    private void savePinFile(File f, UUID uuid, String name, String pin) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(f))) {
            out.writeByte(VERSION_SHA256);
            out.writeUTF(hashPinSha256(pin));
            out.writeUTF(name);
        }
    }

    private String hashPinSha256(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(("maxnetwork:" + pin).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String hashPinLegacy(String pin) {
        return Integer.toHexString(pin.hashCode());
    }

    public void cleanup(UUID uuid) {
        authStates.remove(uuid);
    }

    /** Delete a player's .dat file (admin PIN reset). Player must re-register on next login. */
    public boolean resetPin(UUID uuid) {
        authStates.remove(uuid);
        File f = new File(dataFolder, uuid + ".dat");
        return !f.exists() || f.delete();
    }

    static class AuthState {
        enum Type { REGISTER, LOGIN }
        Type type;
        int attemptsLeft;
        AuthState(Type type, int attemptsLeft) {
            this.type = type;
            this.attemptsLeft = attemptsLeft;
        }
    }
}
