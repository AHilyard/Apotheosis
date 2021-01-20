package shadows.apotheosis.spawn.modifiers;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import shadows.apotheosis.spawn.spawner.ApothSpawnerTile;

public class ConditionModifier extends SpawnerModifier {

	public ConditionModifier() {
		super(new ItemStack(Items.CHORUS_FRUIT), -1, -1, -1);
	}

	@Override
	public boolean canModify(ApothSpawnerTile spawner, ItemStack stack, boolean inverting) {
		return super.canModify(spawner, stack, inverting) && spawner.ignoresConditions == inverting;
	}

	@Override
	public boolean modify(ApothSpawnerTile spawner, ItemStack stack, boolean inverting) {
		spawner.ignoresConditions = !inverting;
		return true;
	}

	@Override
	public String getCategory() {
		return "ignore_spawn_conditions";
	}

	@Override
	public String getDefaultItem() {
		return Blocks.DRAGON_EGG.getRegistryName().toString();
	}

}