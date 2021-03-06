package com.bergerkiller.bukkit.common.reflection.classes;

import java.util.List;

import net.minecraft.server.v1_4_5.IWorldAccess;
import net.minecraft.server.v1_4_5.IntHashMap;
import net.minecraft.server.v1_4_5.PlayerManager;
import net.minecraft.server.v1_4_5.World;
import net.minecraft.server.v1_4_5.WorldServer;

import com.bergerkiller.bukkit.common.reflection.FieldAccessor;
import com.bergerkiller.bukkit.common.reflection.SafeField;

public class WorldServerRef {
	public static final FieldAccessor<PlayerManager> playerManager = new SafeField<PlayerManager>(WorldServer.class, "manager");
	public static final FieldAccessor<List<IWorldAccess>> accessList = new SafeField<List<IWorldAccess>>(World.class, "w");
	public static final FieldAccessor<IntHashMap> entitiesById = new SafeField<IntHashMap>(WorldServer.class, "entitiesById");
}
