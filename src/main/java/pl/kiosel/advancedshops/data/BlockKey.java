package pl.kiosel.advancedshops.data;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;
import java.util.UUID;

public final class BlockKey {

	@Getter
	private final UUID world;
	@Getter
	private final int x, y, z;

	public BlockKey(UUID world, int x, int y, int z) {
		this.world = Objects.requireNonNull(world);
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static BlockKey of(Block block) {
		return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
	}

	public Block resolve() {
		World loaded = Bukkit.getWorld(world);
		return loaded != null && loaded.isChunkLoaded(x >> 4, z >> 4) ? loaded.getBlockAt(x, y, z) : null;
	}

	public Location getLocation() {
		World loaded = Bukkit.getWorld(world);
		return loaded == null ? null : new Location(loaded, x, y, z);
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof BlockKey)) return false;
		BlockKey other = (BlockKey) object;
		return x == other.x && y == other.y && z == other.z && world.equals(other.world);
	}

	@Override
	public int hashCode() {
		return Objects.hash(world, x, y, z);
	}
}