package shadows.apotheosis.adventure.boss;

import java.util.Random;
import java.util.function.BiPredicate;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import shadows.apotheosis.Apotheosis;
import shadows.apotheosis.adventure.AdventureConfig;
import shadows.apotheosis.adventure.AdventureModule;
import shadows.apotheosis.adventure.client.BossSpawnMessage;
import shadows.placebo.network.PacketDistro;

public class BossEvents {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void naturalBosses(LivingSpawnEvent.CheckSpawn e) {
		if (e.getSpawnReason() == MobSpawnType.NATURAL || e.getSpawnReason() == MobSpawnType.CHUNK_GENERATION) {
			LivingEntity entity = e.getEntityLiving();
			Random rand = e.getWorld().getRandom();
			if (!e.getWorld().isClientSide() && entity instanceof Monster && e.getResult() != Result.DENY) {
				ServerLevelAccessor sLevel = (ServerLevelAccessor) e.getWorld();
				Pair<Float, BossSpawnRules> rules = AdventureConfig.BOSS_SPAWN_RULES.get(sLevel.getLevel().dimension().location());
				if (rules == null) return;
				if (rand.nextFloat() <= rules.getLeft() && rules.getRight().test(sLevel, new BlockPos(e.getX(), e.getY(), e.getZ()))) {
					Player player = sLevel.getNearestPlayer(e.getX(), e.getY(), e.getZ(), -1, false);
					if (player == null) return; //Should never be null, but we check anyway since nothing makes sense around here.
					BossItem item = BossItemManager.INSTANCE.getRandomItem(rand, player.getLuck(), sLevel);
					Mob boss = item.createBoss(sLevel, new BlockPos(e.getX() - 0.5, e.getY(), e.getZ() - 0.5), rand, player.getLuck());
					if (canSpawn(sLevel, boss, player.distanceToSqr(boss))) {
						boss.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400));
						sLevel.addFreshEntity(boss);
						e.setResult(Result.DENY);
						AdventureModule.debugLog(boss.blockPosition(), "Surface Boss - " + boss.getName().getString());
						sLevel.players().forEach(p -> {
							Vec3 tPos = new Vec3(boss.getX(), AdventureConfig.bossAnnounceIgnoreY ? p.getY() : boss.getY(), boss.getZ());
							if (p.distanceToSqr(tPos) <= AdventureConfig.bossAnnounceRange * AdventureConfig.bossAnnounceRange) {
								((ServerPlayer) p).connection.send(new ClientboundSetActionBarTextPacket(new TranslatableComponent("info.apotheosis.boss_spawn", boss.getCustomName(), (int) boss.getX(), (int) boss.getY())));
								PacketDistro.sendTo(Apotheosis.CHANNEL, new BossSpawnMessage(boss.blockPosition(), boss.getCustomName().getStyle().getColor().getValue()), player);
							}
						});
						if (AdventureConfig.bossAnnounceSound) sLevel.playSound(null, boss.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, AdventureConfig.bossAnnounceRange / 16, 1.25F);
					}
				}
			}
		}
	}

	private static boolean canSpawn(LevelAccessor world, Mob entity, double playerDist) {
		if (playerDist > entity.getType().getCategory().getDespawnDistance() * entity.getType().getCategory().getDespawnDistance() && entity.removeWhenFarAway(playerDist)) {
			return false;
		} else {
			return entity.checkSpawnRules(world, MobSpawnType.NATURAL) && entity.checkSpawnObstruction(world);
		}
	}

	public static enum BossSpawnRules implements BiPredicate<ServerLevelAccessor, BlockPos> {
		NEEDS_SKY((level, pos) -> level.canSeeSky(pos)),
		NEEDS_SURFACE(
				(level, pos) -> pos.getY() >= level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ())),
		ANY((level, pos) -> true);

		BiPredicate<ServerLevelAccessor, BlockPos> pred;

		private BossSpawnRules(BiPredicate<ServerLevelAccessor, BlockPos> pred) {
			this.pred = pred;
		}

		@Override
		public boolean test(ServerLevelAccessor t, BlockPos u) {
			return pred.test(t, u);
		}
	}

}
