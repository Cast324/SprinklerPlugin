package com.michaelblades;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SprinklerConfig {

    private List<SprinklerEntry> sprinklers = new ArrayList<>();
    private final Path configPath;

    public SprinklerConfig(Path modConfigDirectory) {
        this.configPath = modConfigDirectory.resolve("sprinklers.json");
    }

    public List<SprinklerEntry> getSprinklers() {
        return sprinklers;
    }

    public void setSprinklers(List<SprinklerEntry> sprinklers) {
        this.sprinklers = sprinklers;
    }

    /**
     * Add a sprinkler to the config
     */
    public void addSprinkler(SprinklerEntry entry) {
        // Remove existing sprinkler at same location if present
        removeSprinkler(entry.getWorldId(), entry.getX(), entry.getY(), entry.getZ());
        sprinklers.add(entry);
        save();
    }

    /**
     * Remove a sprinkler at the given location
     */
    public boolean removeSprinkler(String worldId, int x, int y, int z) {
        boolean removed = sprinklers.removeIf(s ->
                s.getWorldId().equals(worldId) &&
                        s.getX() == x &&
                        s.getY() == y &&
                        s.getZ() == z
        );
        if (removed) {
            save();
        }
        return removed;
    }

    /**
     * Find a sprinkler at the given location
     */
    public Optional<SprinklerEntry> getSprinkler(String worldId, int x, int y, int z) {
        return sprinklers.stream()
                .filter(s -> s.getWorldId().equals(worldId) &&
                        s.getX() == x &&
                        s.getY() == y &&
                        s.getZ() == z)
                .findFirst();
    }

    /**
     * Load sprinklers from JSON file
     */
    public void load() {
        try {
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();

                sprinklers.clear();

                if (root.has("sprinklers")) {
                    JsonArray sprinklersArray = root.getAsJsonArray("sprinklers");
                    for (int i = 0; i < sprinklersArray.size(); i++) {
                        JsonObject sprinklerObj = sprinklersArray.get(i).getAsJsonObject();

                        String worldId = sprinklerObj.get("worldId").getAsString();
                        int x = sprinklerObj.get("x").getAsInt();
                        int y = sprinklerObj.get("y").getAsInt();
                        int z = sprinklerObj.get("z").getAsInt();

                        sprinklers.add(new SprinklerEntry(worldId, x, y, z));
                    }
                }

                System.out.println("Loaded " + sprinklers.size() + " sprinklers from config");
            } else {
                System.out.println("No existing config found, using defaults");
            }
        } catch (IOException e) {
            System.err.println("Failed to load sprinkler config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save sprinklers to JSON file
     */
    public void save() {
        try {
            // Ensure the directory exists
            Files.createDirectories(configPath.getParent());

            // Build JSON
            JsonObject root = new JsonObject();
            JsonArray sprinklersArray = new JsonArray();

            for (SprinklerEntry entry : sprinklers) {
                JsonObject sprinklerObj = new JsonObject();
                sprinklerObj.addProperty("worldId", entry.getWorldId());
                sprinklerObj.addProperty("x", entry.getX());
                sprinklerObj.addProperty("y", entry.getY());
                sprinklerObj.addProperty("z", entry.getZ());

                sprinklersArray.add(sprinklerObj);
            }

            root.add("sprinklers", sprinklersArray);

            // Write to file
            Files.writeString(configPath, root.toString());
            System.out.println("Saved " + sprinklers.size() + " sprinklers to config");
        } catch (IOException e) {
            System.err.println("Failed to save sprinkler config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class SprinklerEntry {
        private String worldId;
        private int x;
        private int y;
        private int z;

        public SprinklerEntry() {
        }

        public SprinklerEntry(String worldId, int x, int y, int z) {
            this.worldId = worldId;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String getWorldId() {
            return worldId;
        }

        public void setWorldId(String worldId) {
            this.worldId = worldId;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getZ() {
            return z;
        }

        public void setZ(int z) {
            this.z = z;
        }

        @Override
        public String toString() {
            return "SprinklerEntry{" +
                    "worldId='" + worldId + '\'' +
                    ", x=" + x +
                    ", y=" + y +
                    ", z=" + z +
                    '}';
        }
    }
}