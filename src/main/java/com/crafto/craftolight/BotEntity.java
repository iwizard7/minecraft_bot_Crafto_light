package com.crafto.craftolight;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;

public class BotEntity extends PathfinderMob {
    private Player owner;
    private Task currentTask = Task.NONE;
    private int taskCount = 0;

    public enum Task {
        NONE, MINE_DIAMOND, BUILD_HOUSE, FOLLOW_PLAYER
    }

    public BotEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public Player getOwner() {
        return owner;
    }

    public void setTask(Task task, int count) {
        this.currentTask = task;
        this.taskCount = count;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MineGoal(this));
        this.goalSelector.addGoal(2, new BuildGoal(this));
        this.goalSelector.addGoal(3, new FollowGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    private static class MineGoal extends Goal {
        private final BotEntity bot;

        public MineGoal(BotEntity bot) {
            this.bot = bot;
        }

        @Override
        public boolean canUse() {
            return bot.currentTask == Task.MINE_DIAMOND && bot.taskCount > 0;
        }

        @Override
        public void tick() {
            // Simple mining: break stone nearby
            for (int x = -5; x <= 5; x++) {
                for (int y = -5; y <= 5; y++) {
                    for (int z = -5; z <= 5; z++) {
                        BlockPos pos = bot.blockPosition().offset(x, y, z);
                        if (bot.level().getBlockState(pos).getBlock() == Blocks.STONE) {
                            bot.level().destroyBlock(pos, true);
                            bot.taskCount--;
                            if (bot.taskCount <= 0) {
                                bot.currentTask = Task.NONE;
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    private static class BuildGoal extends Goal {
        private final BotEntity bot;

        public BuildGoal(BotEntity bot) {
            this.bot = bot;
        }

        @Override
        public boolean canUse() {
            return bot.currentTask == Task.BUILD_HOUSE;
        }

        @Override
        public void tick() {
            BlockPos base = bot.blockPosition();
            // First, level the area: clear 11x11 area around base, y=0 to 5
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    for (int y = 0; y <= 5; y++) {
                        BlockPos pos = base.offset(x, y, z);
                        if (!bot.level().getBlockState(pos).isAir()) {
                            bot.level().destroyBlock(pos, true);
                        }
                    }
                    // Place dirt at y=0 for flat ground
                    BlockPos groundPos = base.offset(x, 0, z);
                    bot.level().setBlock(groundPos, Blocks.DIRT.defaultBlockState(), 3);
                }
            }
            // Build a house: 9x9 base, 3 high walls, roof
            // Floor
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = base.offset(x, 0, z);
                    if (bot.level().getBlockState(pos).isAir()) {
                        bot.level().setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                    }
                }
            }
            // Walls
            for (int y = 1; y <= 3; y++) {
                for (int x = -4; x <= 4; x++) {
                    for (int z = -4; z <= 4; z++) {
                        if (x == 0 && z == -4 && y <= 2) continue; // door opening
                        if (Math.abs(x) == 4 || Math.abs(z) == 4) {
                            BlockPos pos = base.offset(x, y, z);
                            if (bot.level().getBlockState(pos).isAir()) {
                                // Add windows (glass) in the middle of walls at y=2
                                if (y == 2 && x == 0 && (z == -3 || z == 3) || z == 0 && (x == -3 || x == 3)) {
                                    bot.level().setBlock(pos, Blocks.GLASS_PANE.defaultBlockState(), 3);
                                } else {
                                    bot.level().setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                                }
                            }
                        }
                    }
                }
            }
            // Roof
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = base.offset(x, 4, z);
                    if (bot.level().getBlockState(pos).isAir()) {
                        bot.level().setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                    }
                }
            }
            // Inside: bed, chest, torch
            BlockPos bedPos = base.offset(1, 1, 1);
            if (bot.level().getBlockState(bedPos).isAir()) {
                bot.level().setBlock(bedPos, Blocks.WHITE_BED.defaultBlockState(), 3);
            }
            BlockPos chestPos = base.offset(-1, 1, 1);
            if (bot.level().getBlockState(chestPos).isAir()) {
                bot.level().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
            }
            BlockPos torchPos = base.offset(0, 3, 0);
            if (bot.level().getBlockState(torchPos).isAir()) {
                bot.level().setBlock(torchPos, Blocks.TORCH.defaultBlockState(), 3);
            }
            // Door
            BlockPos doorPos = base.offset(0, 1, -2);
            if (bot.level().getBlockState(doorPos).isAir()) {
                bot.level().setBlock(doorPos, Blocks.OAK_DOOR.defaultBlockState(), 3);
            }
            bot.currentTask = Task.NONE;
        }
    }

    private static class FollowGoal extends Goal {
        private final BotEntity bot;

        public FollowGoal(BotEntity bot) {
            this.bot = bot;
        }

        @Override
        public boolean canUse() {
            return bot.currentTask == Task.FOLLOW_PLAYER;
        }

        @Override
        public void tick() {
            if (bot.owner != null) {
                bot.getNavigation().moveTo(bot.owner, 1.0);
            }
        }
    }
}
