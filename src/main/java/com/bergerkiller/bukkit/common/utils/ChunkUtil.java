package com.bergerkiller.bukkit.common.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;

import net.minecraft.server.v1_4_5.Chunk;
import net.minecraft.server.v1_4_5.ChunkSection;
import net.minecraft.server.v1_4_5.WorldServer;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_4_5.util.LongHash;
import org.bukkit.craftbukkit.v1_4_5.util.LongHashSet;
import org.bukkit.craftbukkit.v1_4_5.util.LongObjectHashMap;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.internal.CommonPlugin;
import com.bergerkiller.bukkit.common.natives.NativeChunkEntitiesWrapper;
import com.bergerkiller.bukkit.common.natives.NativeChunkWrapper;
import com.bergerkiller.bukkit.common.reflection.classes.ChunkProviderServerRef;
import com.bergerkiller.bukkit.common.reflection.classes.ChunkRef;
import com.bergerkiller.bukkit.common.reflection.classes.ChunkSectionRef;

/**
 * Contains utilities to get and set chunks of a world
 */
public class ChunkUtil {
	private static boolean canUseLongObjectHashMap = CommonUtil.getClass(Common.CB_ROOT + ".util.LongObjectHashMap") != null;
	private static boolean canUseLongHashSet = CommonUtil.getClass(Common.CB_ROOT + ".util.LongHashSet") != null;

	/**
	 * Gets the height of a given column in a chunk
	 * 
	 * @param chunk the column is in
	 * @param x-coordinate of the block column
	 * @param z-coordinate of the block column
	 * @return column height
	 */
	public static int getHeight(org.bukkit.Chunk chunk, int x, int z) {
		return ChunkRef.getHeight(NativeUtil.getNative(chunk), x, z);
	}

	/**
	 * Gets the block light level
	 * 
	 * @param chunk the block is in
	 * @param x-coordinate of the block
	 * @param y-coordinate of the block
	 * @param z-coordinate of the block
	 * @return Block light level
	 */
	public static int getBlockLight(org.bukkit.Chunk chunk, int x, int y, int z) {
		return ChunkRef.getBlockLight(NativeUtil.getNative(chunk), x, y, z);
	}

	/**
	 * Gets the sky light level
	 * 
	 * @param chunk the block is in
	 * @param x-coordinate of the block
	 * @param y-coordinate of the block
	 * @param z-coordinate of the block
	 * @return Sky light level
	 */
	public static int getSkyLight(org.bukkit.Chunk chunk, int x, int y, int z) {
		return ChunkRef.getSkyLight(NativeUtil.getNative(chunk), x, y, z);
	}

	/**
	 * Gets the block data
	 * 
	 * @param chunk the block is in
	 * @param x-coordinate of the block
	 * @param y-coordinate of the block
	 * @param z-coordinate of the block
	 * @return block data
	 */
	public static int getBlockData(org.bukkit.Chunk chunk, int x, int y, int z) {
		return ChunkRef.getData(NativeUtil.getNative(chunk), x, y, z);
	}

	/**
	 * Gets the block type Id
	 * 
	 * @param chunk the block is in
	 * @param x-coordinate of the block
	 * @param y-coordinate of the block
	 * @param z-coordinate of the block
	 * @return block type Id
	 */
	public static int getBlockTypeId(org.bukkit.Chunk chunk, int x, int y, int z) {
		return ChunkRef.getTypeId(NativeUtil.getNative(chunk), x, y, z);
	}

	/**
	 * Sets a block type id and data without causing physics or lighting updates
	 * 
	 * @param chunk the block is in
	 * @param x-coordinate of the block
	 * @param y-coordinate of the block
	 * @param z-coordinate of the block
	 * @param typeId to set to
	 * @param data to set to
	 */
	public static void setBlockFast(org.bukkit.Chunk chunk, int x, int y, int z, int typeId, int data) {
		if (y < 0 || y >= chunk.getWorld().getMaxHeight()) {
			return;
		}
		ChunkSection[] sections = ChunkRef.getSections(NativeUtil.getNative(chunk));
		final int secIndex = y >> 4;
		ChunkSection section = sections[secIndex];
		if (section == null) {
			section = sections[secIndex] = new ChunkSection(y >> 4 << 4);
		}
		ChunkSectionRef.setTypeId(section, x, y, z, typeId);
		ChunkSectionRef.setData(section, x, y, z, data);
	}

	/**
	 * Sets a block type id and data, causing physics and lighting updates
	 * 
	 * @param chunk the block is in
	 * @param x-coordinate of the block
	 * @param y-coordinate of the block
	 * @param z-coordinate of the block
	 * @param typeId to set to
	 * @param data to set to
	 * @return True if a block got changed, False if not
	 */
	public static boolean setBlock(org.bukkit.Chunk chunk, int x, int y, int z, int typeId, int data) {
		boolean result = y >= 0 && y <= chunk.getWorld().getMaxHeight();
		WorldServer world = NativeUtil.getNative(chunk.getWorld());
		if (result) {
			result = ChunkRef.setBlock(NativeUtil.getNative(chunk), x, y, z, typeId, data);
            world.methodProfiler.a("checkLight");
            world.z(x, y, z);
            world.methodProfiler.b();
		}
		if (result) {
			world.applyPhysics(x, y, z, typeId);
		}
		return result;
	}

	/**
	 * Gets a live collection of all the entities in a chunk<br>
	 * Changes to this collection are reflected back in the chunk
	 * 
	 * @param chunk for which to get the entities
	 * @return Live collection of entities in the chunk
	 */
	public static Collection<org.bukkit.entity.Entity> getEntities(org.bukkit.Chunk chunk) {
		return new NativeChunkEntitiesWrapper(chunk);
	}

	/**
	 * Gets all the chunks loaded on a given world
	 * 
	 * @param chunkprovider to get the loaded chunks from
	 * @return Loaded chunks
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Collection<org.bukkit.Chunk> getChunks(World world) {
		if (canUseLongObjectHashMap) {
			Object chunks = ChunkProviderServerRef.chunks.get(NativeUtil.getNative(world).chunkProviderServer);
			if (chunks != null) {
				try {
					if (canUseLongObjectHashMap && chunks instanceof LongObjectHashMap) {
						return new NativeChunkWrapper(((LongObjectHashMap) chunks).values());
					}
				} catch (Throwable t) {
					canUseLongObjectHashMap = false;
					CommonPlugin.getInstance().log(Level.WARNING, "Failed to access chunks using CraftBukkit's long object hashmap, support disabled");
					CommonUtil.filterStackTrace(t).printStackTrace();
				}
			}
		}
		// Bukkit alternative
		return Arrays.asList(world.getLoadedChunks());
	}

	/**
	 * Gets a chunk from a world without loading or generating it
	 * 
	 * @param world to obtain the chunk from
	 * @param x coordinate of the chunk
	 * @param z coordinate of the chunk
	 * @return The chunk, or null if it is not loaded
	 */
	@SuppressWarnings("rawtypes")
	public static org.bukkit.Chunk getChunk(World world, final int x, final int z) {
		final long key = LongHash.toLong(x, z);
		Object chunks = ChunkProviderServerRef.chunks.get(NativeUtil.getNative(world).chunkProviderServer);
		if (chunks != null) {
			if (canUseLongObjectHashMap && chunks instanceof LongObjectHashMap) {
				try {
					return NativeUtil.getChunk(((Chunk) ((LongObjectHashMap) chunks).get(key)));
				} catch (Throwable t) {
					canUseLongObjectHashMap = false;
					CommonPlugin.getInstance().log(Level.WARNING, "Failed to access chunks using CraftBukkit's long object hashmap, support disabled");
					CommonUtil.filterStackTrace(t).printStackTrace();
				}
			}
		}
		// Bukkit alternative
		if (world.isChunkLoaded(x, z)) {
			return world.getChunkAt(x, z);
		} else {
			return null;
		}
	}

	/**
	 * Sets a given chunk coordinate to contain the chunk specified
	 * 
	 * @param world to set the chunk in
	 * @param x coordinate of the chunk
	 * @param z coordinate of the chunk
	 * @param chunk to set to
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void setChunk(World world, final int x, final int z, final org.bukkit.Chunk chunk) {
		if (canUseLongObjectHashMap) {
			Object chunks = ChunkProviderServerRef.chunks.get(NativeUtil.getNative(world).chunkProviderServer);
			if (chunks != null) {
				final long key = LongHash.toLong(x, z);
				try {
					if (canUseLongObjectHashMap && chunks instanceof LongObjectHashMap) {
						((LongObjectHashMap) chunks).put(key, NativeUtil.getNative(chunk));
						return;
					}
				} catch (Throwable t) {
					canUseLongObjectHashMap = false;
					CommonPlugin.getInstance().log(Level.WARNING, "Failed to access chunks using CraftBukkit's long object hashmap, support disabled");
					CommonUtil.filterStackTrace(t).printStackTrace();
				}
			}
		}
		throw new RuntimeException("Failed to set chunk using a known method");
	}

	/**
	 * Saves a single chunk to disk
	 * 
	 * @param chunk to save
	 */
	public static void saveChunk(org.bukkit.Chunk chunk) {
		NativeUtil.getNative(chunk.getWorld()).chunkProviderServer.saveChunk(NativeUtil.getNative(chunk));
	}

	/**
	 * Sets whether a given chunk coordinate has to be unloaded
	 * 
	 * @param world to set the unload request for
	 * @param x coordinate of the chunk
	 * @param z coordinate of the chunk
	 * @param unload state to set to
	 */
	public static void setChunkUnloading(World world, final int x, final int z, boolean unload) {
		if (canUseLongHashSet) {
			Object unloadQueue = ChunkProviderServerRef.unloadQueue.get(NativeUtil.getNative(world).chunkProviderServer);
			if (unloadQueue != null) {
				try {
					if (canUseLongHashSet && unloadQueue instanceof LongHashSet) {
						if (unload) {
							((LongHashSet) unloadQueue).add(x, z);
						} else {
							((LongHashSet) unloadQueue).remove(x, z);
						}
						return;
					}
				} catch (Throwable t) {
					canUseLongHashSet = false;
					CommonPlugin.getInstance().log(Level.WARNING, "Failed to access chunks using CraftBukkit's long object hashmap, support disabled");
					CommonUtil.filterStackTrace(t).printStackTrace();
				}
			}
		}
		throw new RuntimeException("Failed to set unload queue using a known method");
	}
}
