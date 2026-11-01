package pl.kiosel.advancedshops.data.shop;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class PlayerEntityVisibility {

	private static final Method HIDE_ENTITY = method("hideEntity");
	private static final Method SHOW_ENTITY = method("showEntity");

	private PlayerEntityVisibility() {
	}

	static boolean isSupported() {
		return HIDE_ENTITY != null && SHOW_ENTITY != null;
	}

	static void setVisible(Player player, Plugin plugin, Entity entity, boolean visible) {
		Method method = visible ? SHOW_ENTITY : HIDE_ENTITY;
		if (method == null) return;
		try {
			method.invoke(player, plugin, entity);
		} catch (IllegalAccessException error) {
			throw new IllegalStateException("Cannot change per-player entity visibility", error);
		} catch (InvocationTargetException error) {
			Throwable cause = error.getCause();
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			if (cause instanceof Error) throw (Error) cause;
			throw new IllegalStateException("Could not change per-player entity visibility", cause);
		}
	}

	private static Method method(String name) {
		try {
			return Player.class.getMethod(name, Plugin.class, Entity.class);
		} catch (NoSuchMethodException ignored) {
			return null;
		}
	}
}
