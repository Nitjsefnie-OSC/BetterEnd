package org.betterx.betterend.testmod.gametest;

import de.ambertation.wover.test.api.gametest.GameTestAssertions;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Every BetterEnd/BCLib block and item class must survive reflection on a dedicated server.
 * <p>
 * Lithium walks {@code getDeclaredMethods()} of every block class at startup. A member whose signature
 * names a client-only type and is not {@code @Environment(CLIENT)} (so Fabric does not strip it on the
 * server) makes that call fail with "Cannot load class ... in environment type SERVER", and the server
 * does not start. Lambdas count too: javac compiles a lambda inside an {@code @Environment(CLIENT)}
 * method into a synthetic method of the same class, and the stripper leaves that one behind.
 * <p>
 * Some loaders let the reflection itself succeed in dev, so besides catching the failure this also
 * inspects the member types: anything from {@code net.minecraft.client} / {@code com.mojang.blaze3d},
 * or a mod type whose own fields or methods are built on those (e.g. Wover's client-side model
 * generators), is reported.
 */
public class ServerSafeSignatureGameTest {
    private static final Set<String> NAMESPACES = Set.of("betterend", "bclib");

    @GameTest(maxTicks = 200)
    public void blockAndItemClassesHaveNoClientOnlySignatures(GameTestHelper helper) {
        final Set<Class<?>> classes = new LinkedHashSet<>();
        collect(BuiltInRegistries.BLOCK, classes);
        collect(BuiltInRegistries.ITEM, classes);

        final List<String> failures = new ArrayList<>();
        if (classes.size() < 50) {
            failures.add("only found " + classes.size() + " block/item classes - the test setup is broken");
        }
        final Map<Class<?>, Boolean> cache = new HashMap<>();
        for (Class<?> c : classes) check(c, failures, cache);

        GameTestAssertions.failIfAny(
                helper,
                failures.size() + " block/item member(s) would break reflection on a dedicated server"
                        + " - mark them @Environment(EnvType.CLIENT) or move lambdas into a client-only nested class",
                failures
        );
        helper.succeed();
    }

    private static <T> void collect(Registry<T> registry, Set<Class<?>> classes) {
        for (T entry : registry) {
            final var key = registry.getKey(entry);
            if (key == null || !NAMESPACES.contains(key.getNamespace())) continue;
            for (Class<?> c = entry.getClass(); c != null && !isVanillaOrJdk(c); c = c.getSuperclass()) {
                classes.add(c);
            }
        }
    }

    private static void check(Class<?> c, List<String> failures, Map<Class<?>, Boolean> cache) {
        final Method[] methods;
        final Field[] fields;
        final Constructor<?>[] constructors;
        try {
            methods = c.getDeclaredMethods();
            fields = c.getDeclaredFields();
            constructors = c.getDeclaredConstructors();
        } catch (Throwable t) {
            failures.add(c.getName() + ": reflection failed - " + t);
            return;
        }
        for (Method m : methods) {
            final List<Class<?>> types = new ArrayList<>(Arrays.asList(m.getParameterTypes()));
            types.add(m.getReturnType());
            report(c.getName() + "." + m.getName(), types, failures, cache);
        }
        for (Constructor<?> k : constructors) {
            report(c.getName() + ".<init>", Arrays.asList(k.getParameterTypes()), failures, cache);
        }
        for (Field f : fields) {
            report(c.getName() + "." + f.getName(), List.of(f.getType()), failures, cache);
        }
    }

    private static void report(String member, List<Class<?>> types, List<String> failures, Map<Class<?>, Boolean> cache) {
        for (Class<?> t : types) {
            if (isClientOnly(t, cache, 0)) {
                failures.add(member + " references client-only " + t.getName());
                return;
            }
        }
    }

    private static boolean isClientOnly(Class<?> t, Map<Class<?>, Boolean> cache, int depth) {
        while (t.isArray()) t = t.getComponentType();
        if (t.isPrimitive()) return false;
        final String n = t.getName();
        if (n.startsWith("net.minecraft.client.") || n.startsWith("com.mojang.blaze3d.")) return true;
        if (isVanillaOrJdk(t) || depth > 0) return false;
        final Boolean known = cache.get(t);
        if (known != null) return known;
        cache.put(t, false);
        boolean result = false;
        try {
            for (Field f : t.getDeclaredFields()) result |= isClientOnly(f.getType(), cache, depth + 1);
            for (Method m : t.getDeclaredMethods()) {
                result |= isClientOnly(m.getReturnType(), cache, depth + 1);
                for (Class<?> p : m.getParameterTypes()) result |= isClientOnly(p, cache, depth + 1);
            }
        } catch (Throwable e) {
            result = true;
        }
        cache.put(t, result);
        return result;
    }

    private static boolean isVanillaOrJdk(Class<?> c) {
        final String n = c.getName();
        return n.startsWith("net.minecraft.") || n.startsWith("com.mojang.") || n.startsWith("java.")
                || n.startsWith("javax.") || n.startsWith("jdk.") || n.startsWith("net.fabricmc.")
                || n.startsWith("it.unimi.") || n.startsWith("com.google.");
    }
}
