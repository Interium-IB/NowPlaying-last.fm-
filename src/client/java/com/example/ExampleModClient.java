package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.example.nowplaying.NowPlayingHttpServer;
import com.example.nowplaying.NowPlayingService;
import com.example.nowplaying.LastFmPoller;
import com.example.nowplaying.NowPlayingConfig;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Start local endpoint and Last.fm poller client-side as fallback
		try { NowPlayingHttpServer.startIfNotRunning(18080); } catch (Exception ignored) {}
		NowPlayingConfig cfg = NowPlayingConfig.load();
		String apiKey = System.getenv("LASTFM_API_KEY");
		if (apiKey == null || apiKey.isBlank()) apiKey = System.getProperty("lastfm_api_key", cfg.getLastfmApiKey());
		String username = System.getProperty("lastfm_username", cfg.getLastfmUsername());
		LastFmPoller.start(apiKey, username, java.time.Duration.ofSeconds(10));

		// Client-side command when connected to unmodded public servers
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			var root = ClientCommandManager.literal("nowplay")
				.executes(ctx -> executeClientNowPlay())
				.then(ClientCommandManager.literal("g").executes(ctx -> executeClientNowPlayGlobal()))
				.then(ClientCommandManager.literal("lastfm")
					.then(ClientCommandManager.argument("username", com.mojang.brigadier.arguments.StringArgumentType.string())
						.executes(ctx -> executeClientSetUsername(com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "username")))
					)
					.then(ClientCommandManager.literal("api")
						.then(ClientCommandManager.argument("key", com.mojang.brigadier.arguments.StringArgumentType.string())
							.executes(ctx -> executeClientSetApi(com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "key")))
						)
					)
				);
			dispatcher.register(root);
			dispatcher.register(ClientCommandManager.literal("np")
				.executes(c -> executeClientNowPlay())
				.then(ClientCommandManager.literal("g").executes(c -> executeClientNowPlayGlobal()))
			);
		});
	}

	private int executeClientNowPlay() {
		String message = NowPlayingService.getFormattedNowPlaying();
		var client = MinecraftClient.getInstance();
		if (client.player == null) return 1;
		if (message.isEmpty()) {
			client.player.sendMessage(Text.literal("Ничего не играет."), false);
			return 1;
		}
		client.player.networkHandler.sendChatMessage(message);
		return 1;
	}

	private int executeClientNowPlayGlobal() {
		String message = NowPlayingService.getFormattedNowPlaying();
		var client = MinecraftClient.getInstance();
		if (client.player == null) return 1;
		if (message.isEmpty()) {
			client.player.sendMessage(Text.literal("Ничего не играет."), false);
			return 1;
		}
		client.player.networkHandler.sendChatMessage("!" + message);
		return 1;
	}

	private int executeClientSetUsername(String username) {
		var client = MinecraftClient.getInstance();
		NowPlayingConfig cfg = NowPlayingConfig.load();
		cfg.setLastfmUsername(username);
		cfg.save();
		LastFmPoller.updateUsername(username);
		if (client.player != null) {
			client.player.sendMessage(Text.literal("Твой Last.fm ник обновлён: " + username).formatted(Formatting.RED), false);
		}
		return 1;
	}

	private int executeClientSetApi(String key) {
		var client = MinecraftClient.getInstance();
		NowPlayingConfig cfg = NowPlayingConfig.load();
		cfg.setLastfmApiKey(key);
		cfg.save();
		LastFmPoller.updateApiKey(key);
		if (client.player != null) {
			client.player.sendMessage(Text.literal("Last.fm API ключ сохранён.").formatted(Formatting.RED), false);
		}
		return 1;
	}
}