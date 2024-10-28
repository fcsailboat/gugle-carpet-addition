package dev.dubhe.gugle.carpet.tools;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import dev.dubhe.gugle.carpet.GcaExtension;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class FilesUtil<K extends Comparable<K>, V> {
    public final Map<K, V> map = new TreeMap<>();
    public static MinecraftServer server = null;
    private final String gcaJson;
    private static final Gson GSON = GcaExtension.GSON;
    Function<String, K> keyCodec;
    Class<V> vClass;

    public FilesUtil(String jsonPrefix, Function<String, K> keyCodec, Class<V> vClass) {
        this.gcaJson = "%s.gca.json".formatted(jsonPrefix);
        this.keyCodec = keyCodec;
        this.vClass = vClass;
    }

    public void init(@NotNull CommandContext<CommandSourceStack> context) {
        MinecraftServer server1 = context.getSource().getServer();
        this.init(server1);
    }

    public void init(MinecraftServer server1) {
        if (server1 == server) return;
        FilesUtil.server = server1;
        this.map.clear();
        File file = FilesUtil.server.getWorldPath(LevelResource.ROOT).resolve(this.gcaJson).toFile();
        if (!file.isFile()) return;
        try (BufferedReader bfr = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, JsonElement> entry : FilesUtil.GSON.fromJson(bfr, JsonObject.class).entrySet()) {
                this.map.put(keyCodec.apply(entry.getKey()), FilesUtil.GSON.fromJson(entry.getValue(), this.vClass));
            }
        } catch (IOException e) {
            GcaExtension.LOGGER.error(e.getMessage(), e);
        }
    }

    public void save() {
        if (FilesUtil.server == null) return;
        File file = FilesUtil.server.getWorldPath(LevelResource.ROOT).resolve(this.gcaJson).toFile();
        try (BufferedWriter bw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            FilesUtil.GSON.toJson(this.map, bw);
        } catch (IOException e) {
            GcaExtension.LOGGER.error(e.getMessage(), e);
        }
    }
}
