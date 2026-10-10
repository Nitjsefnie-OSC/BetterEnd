package org.betterx.betterend.testmod.gametest;

import org.betterx.bclib.api.v2.levelgen.structures.StructureWorld;
import org.betterx.betterend.registry.EndStructures;
import org.betterx.betterend.registry.block.EndDecorBlocks;
import org.betterx.betterend.world.structures.features.PaintedMountainStructure;
import org.betterx.betterend.world.structures.piece.PaintedMountainPiece;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Regression test for BetterEnd #582 (second part): 26.3 renamed the fields of a serialized block state from
 * {@code {Name, Properties}} to {@code {id, properties}}. Vanilla's datafixer only renames them in vanilla data,
 * so structure pieces saved by 26.2 or older (BCLib's {@link StructureWorld} palette, the slices of a painted
 * mountain) still carry the old names. Read with the 26.3 reader, every one of those states became air, and
 * a half-generated giant ice star was completed with air instead of ice.
 * <p>
 * Both tests save a piece with the current writer, rename the block state fields back to the pre-26.3 names
 * (exactly what a 26.1 world contains) and check that loading yields the original blocks, including their
 * properties.
 */
public class LegacyBlockStateNbtGameTest {

    @GameTest
    public void structureWorldReadsPre263BlockStates(GameTestHelper helper) {
        final BlockState[] palette = {
                EndDecorBlocks.EMERALD_ICE.defaultBlockState(),
                EndDecorBlocks.DENSE_EMERALD_ICE.defaultBlockState(),
                EndDecorBlocks.ANCIENT_EMERALD_ICE.defaultBlockState(),
                EndDecorBlocks.DENSE_SNOW.defaultBlockState(),
                Blocks.OAK_STAIRS.defaultBlockState()
                                 .setValue(StairBlock.FACING, Direction.EAST)
                                 .setValue(StairBlock.HALF, Half.TOP)
        };
        final StructureWorld original = new StructureWorld();
        final RandomSource random = RandomSource.create(582);
        for (int i = 0; i < 400; i++) {
            final BlockPos pos = new BlockPos(random.nextInt(40) - 20, 40 + random.nextInt(60), random.nextInt(40) - 20);
            original.setBlock(pos, palette[random.nextInt(palette.length)]);
        }
        final Map<ChunkPos, Map<BlockPos, BlockState>> expected = voxels(original);

        final CompoundTag compact = original.toBNT();
        final CompoundTag compactPre263 = withLegacyStateFields(compact);
        final CompoundTag perBlockPre263 = withLegacyStateFields(toPerBlockLayout(compact));

        final List<String> failures = new ArrayList<>();
        if (compactPre263.equals(compact)) failures.add("renaming the fields did not change the tag");
        compare("compact layout, 26.3 fields", expected, new StructureWorld(compact), failures);
        compare("compact layout, pre-26.3 fields", expected, new StructureWorld(compactPre263), failures);
        compare("per-block layout, pre-26.3 fields", expected, new StructureWorld(perBlockPre263), failures);
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(String.join("\n", failures)));
        }
        helper.succeed();
    }

    @GameTest
    public void paintedMountainReadsPre263Slices(GameTestHelper helper) {
        final StructurePieceSerializationContext context = StructurePieceSerializationContext.fromLevel(helper.getLevel());
        final Holder<Biome> biome = helper.getLevel()
                                          .registryAccess()
                                          .lookupOrThrow(Registries.BIOME)
                                          .getOrThrow(Biomes.THE_END);
        final BlockState[] slices = Arrays.copyOf(PaintedMountainStructure.VARIANTS, PaintedMountainStructure.VARIANTS.length);
        final PaintedMountainPiece original = new PaintedMountainPiece(
                new BlockPos(0, 64, 0), 24, 48, RandomSource.create(582), biome, slices
        );

        final CompoundTag tag = original.createTag(context);
        final ListTag saved = tag.getListOrEmpty("slises");
        final ListTag legacy = new ListTag();
        for (Tag entry : saved) legacy.add(legacyState((CompoundTag) entry));
        tag.put("slises", legacy);

        final StructurePiece loaded = EndStructures.PAINTED_MOUNTAIN_PIECE.load(context, tag);
        final BlockState[] actual = slices(loaded);
        helper.assertTrue(
                Arrays.equals(slices, actual),
                Component.literal("painted mountain slices saved before 26.3 loaded as " + Arrays.toString(actual)
                        + ", expected " + Arrays.toString(slices))
        );
        helper.succeed();
    }

    /** Renames {@code id/properties} back to {@code Name/Properties} in every palette of a StructureWorld tag. */
    private static CompoundTag withLegacyStateFields(CompoundTag world) {
        final CompoundTag result = world.copy();
        for (Tag p : result.getListOrEmpty("parts")) {
            final CompoundTag part = (CompoundTag) p;
            final ListTag states = new ListTag();
            for (Tag s : part.getListOrEmpty("states")) states.add(legacyState((CompoundTag) s));
            part.put("states", states);
        }
        return result;
    }

    /** A block state as {@code NbtUtils.writeBlockState} wrote it up to 26.2. */
    private static CompoundTag legacyState(CompoundTag modern) {
        final CompoundTag legacy = new CompoundTag();
        legacy.putString("Name", modern.getStringOr("id", ""));
        modern.getCompound("properties").ifPresent(properties -> legacy.put("Properties", properties.copy()));
        return legacy;
    }

    /** The per-block layout BCLib wrote before the compact one (one compound per block). */
    private static CompoundTag toPerBlockLayout(CompoundTag world) {
        final CompoundTag result = world.copy();
        final ListTag parts = new ListTag();
        for (Tag p : world.getListOrEmpty("parts")) {
            final CompoundTag part = (CompoundTag) p;
            final CompoundTag old = new CompoundTag();
            old.putInt("x", part.getIntOr("x", 0));
            old.putInt("z", part.getIntOr("z", 0));
            old.put("states", part.getListOrEmpty("states").copy());
            final ListTag blocks = new ListTag();
            for (int value : part.getIntArray("packed").orElseThrow()) {
                final CompoundTag block = new CompoundTag();
                block.put("pos", new IntArrayTag(new int[]{(value >>> 28) & 15, (value << 8) >> 20, (value >>> 24) & 15}));
                block.putInt("state", value & 0xFFF);
                blocks.add(block);
            }
            old.put("blocks", blocks);
            parts.add(old);
        }
        result.put("parts", parts);
        return result;
    }

    private static void compare(
            String scenario,
            Map<ChunkPos, Map<BlockPos, BlockState>> expected,
            StructureWorld world,
            List<String> failures
    ) {
        final Map<ChunkPos, Map<BlockPos, BlockState>> actual = voxels(world);
        if (actual.equals(expected)) return;
        int wrong = 0;
        int air = 0;
        for (Map.Entry<ChunkPos, Map<BlockPos, BlockState>> e : expected.entrySet()) {
            final Map<BlockPos, BlockState> a = actual.getOrDefault(e.getKey(), Map.of());
            for (Map.Entry<BlockPos, BlockState> b : e.getValue().entrySet()) {
                final BlockState state = a.get(b.getKey());
                if (state != b.getValue()) wrong++;
                if (state != null && state.isAir()) air++;
            }
        }
        failures.add(scenario + ": " + wrong + " blocks differ (" + air + " became air)");
    }

    @SuppressWarnings("unchecked")
    private static Map<ChunkPos, Map<BlockPos, BlockState>> voxels(StructureWorld world) {
        try {
            final Field partsField = StructureWorld.class.getDeclaredField("parts");
            partsField.setAccessible(true);
            final Map<ChunkPos, Object> parts = (Map<ChunkPos, Object>) partsField.get(world);
            final Map<ChunkPos, Map<BlockPos, BlockState>> result = new HashMap<>();
            for (Map.Entry<ChunkPos, Object> e : parts.entrySet()) {
                final Field blocksField = e.getValue().getClass().getDeclaredField("blocks");
                blocksField.setAccessible(true);
                result.put(e.getKey(), new HashMap<>((Map<BlockPos, BlockState>) blocksField.get(e.getValue())));
            }
            return result;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static BlockState[] slices(StructurePiece piece) {
        try {
            final Field field = PaintedMountainPiece.class.getDeclaredField("slices");
            field.setAccessible(true);
            return (BlockState[]) field.get(piece);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
