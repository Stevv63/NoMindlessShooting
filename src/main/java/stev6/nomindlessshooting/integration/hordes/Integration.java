package stev6.nomindlessshooting.integration.hordes;

import net.minecraft.server.level.ServerPlayer;
import net.smileycorp.hordes.hordeevent.HordeSpawnTable;
import net.smileycorp.hordes.hordeevent.capability.HordeEvent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

// ill probably abandon this once many changes happen, its just for fun
public interface Integration {

    Integration INSTANCE = create();

    boolean isActive(HordeEvent e);

    HordeSpawnTable getSpawnTable(HordeEvent e);

    void setSpawnTable(HordeEvent e, HordeSpawnTable table);

    private static Integration create() {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        MethodHandle isActive = findOrThrow(lookup,
                nt("isActive", MethodType.methodType(boolean.class)),
                nt("isActive", MethodType.methodType(boolean.class, ServerPlayer.class)));

        if (isActive.type().parameterCount() == 2) {
            isActive = MethodHandles.insertArguments(isActive, 1, (ServerPlayer) null);
        }

        MethodHandle getSpawnTable = findOrThrow(lookup,
                nt("getSpawnTable", MethodType.methodType(HordeSpawnTable.class)),
                nt("getSpawntable", MethodType.methodType(HordeSpawnTable.class)));

        // this isnt changed to its proper name yet, but it probably will be considering the fact its getter did so i accounted for it
        MethodHandle setSpawnTable = findOrThrow(lookup,
                nt("setSpawntable", MethodType.methodType(void.class, HordeSpawnTable.class)),
                nt("setSpawnTable", MethodType.methodType(void.class, HordeSpawnTable.class)));

        MethodHandle finalIsActive = isActive;

        return new Integration() {
            @Override
            public boolean isActive(HordeEvent e) {
                return (boolean) Integration.invoke(finalIsActive, e);
            }

            @Override
            public HordeSpawnTable getSpawnTable(HordeEvent e) {
                return (HordeSpawnTable) Integration.invoke(getSpawnTable, e);
            }

            @Override
            public void setSpawnTable(HordeEvent e, HordeSpawnTable table) {
                Integration.invoke(setSpawnTable, e, table);
            }
        };
    }

    private static Object invoke(MethodHandle handle, Object args) {
        try {
            return handle.invoke(args);
        } catch (Throwable t) {
            throw new RuntimeException("Hordes " + handle.type().toMethodDescriptorString() + "() invocation failed", t);
        }
    }

    private static Object invoke(MethodHandle handle, Object args, Object args2) {
        try {
            return handle.invoke(args, args2);
        } catch (Throwable t) {
            throw new RuntimeException("Hordes " + handle.type().toMethodDescriptorString() + "() invocation failed", t);
        }
    }

    private static MethodHandle findOrThrow(MethodHandles.Lookup lookup, NameAndType... candidates) {
        for (NameAndType c : candidates) {
            try {
                return lookup.findVirtual(HordeEvent.class, c.name(), c.type());
            } catch (ReflectiveOperationException ignored) {
            }
        }
        throw new IllegalStateException("Unknown Hordes version: no compatible method found among: "
                + java.util.Arrays.toString(candidates));
    }

    private static NameAndType nt(String name, MethodType type) {
        return new NameAndType(name, type);
    }

    record NameAndType(String name, MethodType type) {
    }
}
