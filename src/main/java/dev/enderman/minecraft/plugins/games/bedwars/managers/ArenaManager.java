package dev.enderman.minecraft.plugins.games.bedwars.managers;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.enderman.minecraft.plugins.games.bedwars.BedwarsPlugin;
import dev.enderman.minecraft.plugins.games.bedwars.enums.Team;
import dev.enderman.minecraft.plugins.games.bedwars.types.Arena;
import dev.enderman.minecraft.plugins.games.bedwars.utility.types.BedLocation;
import net.kyori.adventure.text.Component;

import java.util.*;

public final class ArenaManager {
	private final List<Arena> arenas = new ArrayList<>();
	private final Map<Team, BossBar> bossBars = new HashMap<>();
	private final Map<Team, Scoreboard> scoreboards = new HashMap<>();

	public ArenaManager(@NotNull final BedwarsPlugin plugin) {
		final FileConfiguration config = plugin.getConfig();
		final ConfigurationSection arenasList = config.getConfigurationSection("arenas");
		assert arenasList != null;

		for (final String arenaKey : arenasList.getKeys(false)) {
			final String worldName = config.getString("arenas." + arenaKey + ".world-name");
			assert worldName != null;

			final World world = Bukkit.createWorld(
							new WorldCreator(worldName)
			);
			assert world != null;

			world.setAutoSave(false);

			final ConfigurationSection teams = config.getConfigurationSection("arenas." + arenaKey + ".teams");
			assert teams != null;

			final HashMap<Team, Location> spawns = new HashMap<>();
			final HashMap<Team, BedLocation> bedLocations = new HashMap<>();

			for (String team : teams.getKeys(false)) {
				spawns.put(
								Team.valueOf(team.toUpperCase()),
								new Location(
												world,
												config.getDouble("arenas." + arenaKey + ".teams." + team + ".spawn-location.x"),
												config.getDouble("arenas." + arenaKey + ".teams." + team + ".spawn-location.y"),
												config.getDouble("arenas." + arenaKey + ".teams." + team + ".spawn-location.z"),
												(float) config.getDouble("arenas." + arenaKey + ".teams." + team + ".spawn-location.yaw"),
												(float) config.getDouble("arenas." + arenaKey + ".teams." + team + ".spawn-location.pitch")
								)
				);

				final String facing = config.getString("arenas." + arenaKey + ".teams." + team + ".bed-location.facing");
				assert facing != null;

				bedLocations.put(
								Team.valueOf(team.toUpperCase()),
								new BedLocation(
												world,
												config.getDouble("arenas." + arenaKey + ".teams." + team + ".bed-location.x"),
												config.getDouble("arenas." + arenaKey + ".teams." + team + ".bed-location.y"),
												config.getDouble("arenas." + arenaKey + ".teams." + team + ".bed-location.z"),
												BlockFace.valueOf(facing.toUpperCase())
								)
				);
			}

			final String spawnWorldName = config.getString("arenas." + arenaKey + ".spawn-location.world-name");
			assert spawnWorldName != null;

			final String npcWorldName = config.getString("arenas." + arenaKey + ".npc-location.world-name");
			assert npcWorldName != null;

			// Idea: make a utility method for this:
			arenas.add(
							new Arena(
											plugin,
											Integer.parseInt(arenaKey),
											new Location(
															Bukkit.getWorld(spawnWorldName),
															config.getDouble("arenas." + arenaKey + ".spawn-location.x"),
															config.getDouble("arenas." + arenaKey + ".spawn-location.y"),
															config.getDouble("arenas." + arenaKey + ".spawn-location.z"),
															(float) config.getDouble("arenas." + arenaKey + ".spawn-location.yaw"),
															(float) config.getDouble("arenas." + arenaKey + ".spawn-location.pitch")

											),
											spawns,
											bedLocations,
											new Location(
															Bukkit.getWorld(npcWorldName),
															config.getDouble("arenas." + arenaKey + ".npc-location.x"),
															config.getDouble("arenas." + arenaKey + ".npc-location.y"),
															config.getDouble("arenas." + arenaKey + ".npc-location.z"),
															(float) config.getDouble("arenas." + arenaKey + ".npc-location.yaw"),
															(float) config.getDouble("arenas." + arenaKey + ".npc-location.pitch")
											),
											config.getDouble("arenas." + arenaKey + ".void-level")
							)
			);
		}

		for (final Team team : Team.values()) {
			bossBars.put(
							team,
							Bukkit.createBossBar(
											ChatColor.RED.toString() + ChatColor.BOLD + "Bedwars",
											team.getBossBarColour(),
											BarStyle.SEGMENTED_12
							)
			);

			final ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
			assert scoreboardManager != null;

			final Scoreboard scoreboard = scoreboardManager.getNewScoreboard();

			Objective objective = scoreboard.registerNewObjective("bedwars_scoreboard", Criteria.DUMMY, "dummy");
			objective.setDisplaySlot(DisplaySlot.SIDEBAR);
			objective.displayName(Component.text(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Bedwars")); // Colour codes count as 2 characters. No character limit.

			// Every line has to be unique.

			objective.getScore(ChatColor.GRAY + "localhost " + ChatColor.DARK_GRAY + "| " + ChatColor.GRAY + "0").setScore(1);
			objective.getScore("").setScore(2);

			// Red ✔
			// Yellow ✘
			// Green ✔
			// Blue ✘

			int index = 3;

			for (; index < 3 + Team.values().length; index++) {
				final Team currentTeam = Team.values()[index - 3];

				final org.bukkit.scoreboard.Team teamBedStatus = scoreboard.registerNewTeam(currentTeam.name().toLowerCase() + "_team_bed_status");
				teamBedStatus.addEntry(currentTeam.getColour().toString());

				teamBedStatus.prefix(Component.text(currentTeam.getColour() + currentTeam.getName() + " ")); // 1.13+ -> No character limit.
				teamBedStatus.suffix(Component.text(ChatColor.GREEN + "✔"));

				objective.getScore(currentTeam.getColour().toString()).setScore(index);
			}

			objective.getScore(" ").setScore(index + 1);
			objective.getScore(ChatColor.GRAY + "Team: " + team.getColour() + team.getName()).setScore(index + 2); // 144 character limit.
			objective.getScore("  ").setScore(index + 3);

			scoreboards.put(
							team,
							scoreboard
			);
		}
	}

	public List<Arena> getArenas() {
		return arenas;
	}

	public Map<Team, BossBar> getBossBars() {
		return bossBars;
	}

	public Map<Team, Scoreboard> getScoreboards() {
		return scoreboards;
	}

	public @Nullable Arena getArenaByPlayerUUID(@NotNull final UUID playerUUID) {
		for (final Arena arena : arenas) {
			if (arena.getPlayers().contains(playerUUID)) {
				return arena;
			}
		}

		return null;
	}

	public @Nullable Arena getArena(final int id) {
		for (final Arena arena : arenas) {
			if (arena.getID() == id) {
				return arena;
			}
		}

		return null;
	}

	public @Nullable Arena getArenaByEntityUUID(@NotNull final UUID entityUUID) {
		for (final Arena arena : arenas) {
			if (arena.getVillager().getUniqueId().equals(entityUUID)) {
				return arena;
			}
		}

		return null;
	}

	public @Nullable Arena getArena(@NotNull final World world) {
		for (final Arena arena : arenas) {
			final World arenaWorld = arena.getSpawnLocation().getWorld();
			assert arenaWorld != null;

			if (arenaWorld.getName().equals(world.getName())) {
				return arena;
			}
		}

		return null;
	}
}
