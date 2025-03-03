package net.minestom.generators;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minestom.datagen.DataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

public final class SoundGenerator extends DataGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoundGenerator.class);

    @Override
    public JsonObject generate() {
        JsonObject sounds = new JsonObject();
        var registry = BuiltInRegistries.SOUND_EVENT;

        try (FileReader reader = new FileReader(Paths.get("mappings", "sound.json").toFile())) {
            JsonObject mapping = JsonParser.parseReader(reader).getAsJsonObject();

            for (var soundEvent : registry) {
                final var javaLocation = registry.getKey(soundEvent);
                if (javaLocation == null) {
                    LOGGER.error("Null registry key for sound event: {}", soundEvent);
                    continue;
                }

                final var javaValue = javaLocation.getPath().replace("minecraft:", "");
                if (!mapping.has(javaValue)) {
                    LOGGER.warn("Missing mapping for sound: {}", javaValue);
                    continue;
                }

                final var bedrockValues = mapping.getAsJsonObject(javaValue);
                final var bedrockLocation = bedrockValues.get("playsound_mapping").getAsString();
                final var bedrockSoundType = bedrockValues.get("bedrock_mapping").getAsString();

                JsonObject sound = new JsonObject();
                sound.addProperty("soundtype", bedrockSoundType);

                sounds.add(bedrockLocation, sound);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load mappings/sounds.json. Does the file even exist?", e);
        }

        return sounds;
    }

}
