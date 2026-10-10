package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.BetterEnd;
import de.ambertation.wover.structure.api.StructureNBT;
import de.ambertation.wover.test.api.gametest.GameTestAssertions;
import de.ambertation.wover.test.api.gametest.StructureTemplateAudit;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * BetterEnd's structure templates must place exactly what they were authored to place.
 * <p>
 * Regression test for the 26.3 port: BCLib's {@code StructureHelper} (since removed) read templates without
 * the {@code STRUCTURE} DataFixer pass, and 26.3 renamed the palette keys ({@code Name}/{@code Properties} to
 * {@code id}/{@code properties}), so every pre-26.3 template loaded as all air - only its entities survived.
 * The End pillars and the exit portal vanished while the crystals kept spawning, in mid-air. Three loaders
 * existed then, and the DataFixer fix had reached only one of them; they were merged into
 * {@link StructureNBT}.
 * <p>
 * The checks themselves live in {@link StructureTemplateAudit}, shared with BetterNether:
 * <ul>
 *     <li>a template still in the 1.16 format (a test-only copy of an old pillar top) loads like vanilla - this
 *     is what fails if the loader skips the DataFixer;</li>
 *     <li>every shipped template loads like vanilla, no block in it loads as air, and its block entity data
 *     carries the id of the block entity its block creates;</li>
 *     <li>every shipped template is at the current data version, so a Minecraft update flags templates to
 *     upgrade with WunderVault's {@code structuregen/upgrade_structures.sh}.</li>
 * </ul>
 */
public class StructureTemplateLoadGameTest {
    private static final Identifier LEGACY_TEMPLATE = Identifier.fromNamespaceAndPath(
            "betterend_testmod",
            "legacy_pillar_top"
    );

    @GameTest(maxTicks = 200)
    public void legacyTemplateLoadsLikeVanilla(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final List<String> failures = new ArrayList<>();
        try {
            final int version = NbtUtils.getDataVersion(StructureTemplateAudit.readRaw(level, LEGACY_TEMPLATE), 500);
            if (version >= StructureTemplateAudit.currentDataVersion()) {
                failures.add(LEGACY_TEMPLATE + " is at data version " + version
                        + ", it has to stay an outdated file for this test to mean anything");
            }
        } catch (IOException e) {
            failures.add(LEGACY_TEMPLATE + ": cannot read (" + e.getMessage() + ")");
        }
        final String diff = StructureTemplateAudit.loadsLikeVanilla(level, LEGACY_TEMPLATE);
        if (diff != null) failures.add(diff);
        GameTestAssertions.failIfAny(helper, "legacy template", failures);
        helper.succeed();
    }

    @GameTest(maxTicks = 2000)
    public void shippedTemplatesPlaceWhatTheyShould(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final List<Identifier> templates = StructureTemplateAudit.shippedTemplates(level, BetterEnd.C.namespace);
        final List<String> failures = new ArrayList<>();
        if (templates.size() < 50) {
            failures.add("only found " + templates.size() + " BetterEnd structure templates, expected well over 100");
        }
        for (Identifier id : templates) {
            final String diff = StructureTemplateAudit.loadsLikeVanilla(level, id);
            if (diff != null) failures.add(diff);
            failures.addAll(StructureTemplateAudit.blocksLoadingAsAir(level, id));
            failures.addAll(StructureTemplateAudit.mismatchedBlockEntityIds(level, id));
        }
        GameTestAssertions.failIfAny(helper, failures.size() + " problem(s) in BetterEnd's templates", failures);
        helper.succeed();
    }

    @GameTest(maxTicks = 2000)
    public void shippedTemplatesAreAtCurrentDataVersion(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final List<String> failures = StructureTemplateAudit.outdatedDataVersions(
                level,
                StructureTemplateAudit.shippedTemplates(level, BetterEnd.C.namespace)
        );
        if (!failures.isEmpty()) {
            failures.add("upgrade them with WunderVault's structuregen/upgrade_structures.sh --apply");
        }
        GameTestAssertions.failIfAny(helper, "outdated BetterEnd templates", failures);
        helper.succeed();
    }
}
