package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.example.nowplaying.NowPlayingHttpServer;
import com.example.nowplaying.NowPlayingService;
import com.example.nowplaying.LastFmPoller;
import com.example.nowplaying.NowPlayingConfig;
import com.example.nowplaying.PlayerConfigStore;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "modid";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		// Defer starting services to server start to ensure server-side state
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			try {
				NowPlayingHttpServer.startIfNotRunning(18080);
				LOGGER.info("NowPlaying HTTP server started on http://127.0.0.1:18080");
			} catch (Exception e) {
				LOGGER.error("Failed to start NowPlaying HTTP server", e);
			}
			NowPlayingConfig cfg = NowPlayingConfig.load();
			String apiKey = System.getenv("LASTFM_API_KEY");
			if (apiKey == null || apiKey.isBlank()) apiKey = System.getProperty("lastfm_api_key", cfg.getLastfmApiKey());
			String username = System.getProperty("lastfm_username", cfg.getLastfmUsername());
			LastFmPoller.start(apiKey, username, java.time.Duration.ofSeconds(10));
		});

		// Register /nowplay command
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LOGGER.info("Registering /nowplay command (env: {})", environment);
			var nowplay = CommandManager.literal("nowplay")
				.requires(src -> true)
				.executes(this::executeNowPlay)
				.then(CommandManager.literal("g").executes(this::executeNowPlayGlobal))
				.then(CommandManager.literal("lastfm")
					.then(CommandManager.argument("username", com.mojang.brigadier.arguments.StringArgumentType.string())
						// allow any player to set local username
						.requires(src -> true)
						.executes(this::executeSetLastfmUsername)
					)
					.then(CommandManager.literal("api")
						.then(CommandManager.argument("key", com.mojang.brigadier.arguments.StringArgumentType.string())
							.requires(src -> true)
							.executes(this::executeSetLastfmApiKey)
						)
					)
				);
			dispatcher.register(nowplay);
			dispatcher.register(CommandManager.literal("np").redirect(nowplay.build()));
		});

	}

	private int executeNowPlay(CommandContext<ServerCommandSource> context) {
		String message = NowPlayingService.getFormattedNowPlaying();
		if (message.isEmpty()) {
			context.getSource().sendFeedback(() -> Text.literal("Ничего не играет."), false);
			return Command.SINGLE_SUCCESS;
		}
		// Broadcast the same message to all players
		var server = context.getSource().getServer();
		var playerManager = server.getPlayerManager();
		Text normal = Text.literal(message);
		playerManager.getPlayerList().forEach(p -> p.sendMessage(normal, false));
		return Command.SINGLE_SUCCESS;
	}

	private int executeNowPlayGlobal(CommandContext<ServerCommandSource> context) {
		String message = NowPlayingService.getFormattedNowPlaying();
		if (message.isEmpty()) {
			context.getSource().sendFeedback(() -> Text.literal("Ничего не играет."), false);
			return Command.SINGLE_SUCCESS;
		}
		var server = context.getSource().getServer();
		var playerManager = server.getPlayerManager();
		Text normal = Text.literal("!" + message);
		playerManager.getPlayerList().forEach(p -> p.sendMessage(normal, false));
		return Command.SINGLE_SUCCESS;
	}

	private int executeSetLastfmUsername(CommandContext<ServerCommandSource> context) {
		String username = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "username");
		var player = context.getSource().getPlayer();
		if (player != null) {
			// Per-player config
			PlayerConfigStore.setLastfmUsername(player.getUuid(), username);
			// Make poller read this player's username when they execute the command
			LastFmPoller.updateUsername(username);
			player.sendMessage(Text.literal("Твой Last.fm ник обновлён: " + username).formatted(Formatting.RED), false);
		} else {
			// Console: write to global config
			NowPlayingConfig cfg = NowPlayingConfig.load();
			cfg.setLastfmUsername(username);
			cfg.save();
			context.getSource().sendFeedback(() -> Text.literal("Глобальный Last.fm ник обновлён: " + username), false);
		}
		return Command.SINGLE_SUCCESS;
	}

	private int executeSetLastfmApiKey(CommandContext<ServerCommandSource> context) {
		String key = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "key");
		LastFmPoller.updateApiKey(key);
		NowPlayingConfig cfg = NowPlayingConfig.load();
		cfg.setLastfmApiKey(key);
		cfg.save();
		var p = context.getSource().getPlayer();
		if (p != null) {
			p.sendMessage(Text.literal("Last.fm API ключ сохранён.").formatted(Formatting.RED), false);
		} else {
			context.getSource().sendFeedback(() -> Text.literal("Last.fm API ключ сохранён."), false);
		}
		return Command.SINGLE_SUCCESS;
	}
}