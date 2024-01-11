package com.github.aws404.polypackhost;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.net.httpserver.HttpServer;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ZipResourcePack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PolypackHttpServer {
    private static HttpServer server = null;
    private static ExecutorService threadPool = null;

    private PolypackHttpServer() {
    }

    public static void stop() {
        server.stop(1);
        threadPool.shutdownNow();
    }

    public static void init(MinecraftDedicatedServer minecraftServer) {
        threadPool = Executors.newFixedThreadPool(PolypackHostMod.CONFIG.threadCount, new ThreadFactoryBuilder().setNameFormat("Polypack-Host-%d").build());

        CompletableFuture.runAsync(() -> {
            try {
                PolypackHostMod.LOGGER.info("Starting Polymer resource pack server...");
                PolymerResourcePackUtils.buildMain(PolypackHostMod.POLYMER_PACK_FILE);

                String serverIp = PolypackHostMod.CONFIG.externalIp.isEmpty() ? minecraftServer.getServerIp() : PolypackHostMod.CONFIG.externalIp;
                if (serverIp.isEmpty()) {
                    PolypackHostMod.LOGGER.warn("No external IP address is defined in the configuration, this may cause issues outside of the local network.");
                    serverIp = InetAddress.getLocalHost().getHostAddress();
                }

                String subUrl = PolypackHostMod.CONFIG.randomiseUrl ? Integer.toString(new Random().nextInt(Integer.MAX_VALUE)) : "pack";

                server = HttpServer.create(new InetSocketAddress("0.0.0.0", PolypackHostMod.CONFIG.hostPort), 0);
                server.createContext("/" + subUrl, PolypackHttpHandler.getHandler());
                server.setExecutor(threadPool);
                server.start();

                String url = String.format("http://%s:%s/%s", serverIp, PolypackHostMod.CONFIG.hostPort, subUrl);
                String hash;
                try (FileInputStream file = new FileInputStream(PolypackHostMod.POLYMER_PACK_FILE.toString())) {
                    hash = String.format("%040x", new BigInteger(1, MessageDigest
                            .getInstance("SHA-1")
                            .digest(file.readAllBytes()))
                    );
                } catch (Exception ignored) {
                    hash = "";
                }

                boolean required = false;
                Text prompt = Text.empty();
                var props = minecraftServer.getProperties().serverResourcePackProperties;
                if (props.isPresent()) {
                    required = props.get().isRequired();
                    prompt = props.get().prompt();
                }
                minecraftServer.getProperties().serverResourcePackProperties = Optional.of(new MinecraftServer.ServerResourcePackProperties(UUID.randomUUID(), url, hash, required, prompt));
                PolypackHostMod.LOGGER.info("Polymer resource pack host started at {} (Hash: {})", url, hash);
            } catch (Exception e) {
                PolypackHostMod.LOGGER.error("Failed to start the resource pack server!", e);
            }
        }, threadPool);
    }
}
