package github.kasuminova.ecoaeextension.common.block.ecotech.ecalculator;

import github.kasuminova.ecoaeextension.common.core.CreativeTabNovaEng;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;

import javax.annotation.Nonnull;

@SuppressWarnings("deprecation")
public abstract class BlockECalculator extends Block {

    protected String harvestTool = "pickaxe";
    protected int harvestLevel = 0;

    protected BlockECalculator() {
        super(Material.IRON);
        this.translucent = true;
        this.fullBlock = false;
        this.lightOpacity = 0;
        this.setHardness(20.0F);
        this.setResistance(2000.0F);
        this.setSoundType(SoundType.METAL);
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
    }

    @Override
    public void setHarvestLevel(String toolClass, int level) {
        this.harvestTool = toolClass;
        this.harvestLevel = level;
    }

    @Override
    public String getHarvestTool(@Nonnull IBlockState state) {
        return harvestTool;
    }

    @Override
    public int getHarvestLevel(@Nonnull IBlockState state) {
        return harvestLevel;
    }

    @Override
    public boolean isOpaqueCube(@Nonnull final IBlockState state) {
        return false;
    }

    @Override
    public boolean canEntitySpawn(@Nonnull final IBlockState state, @Nonnull final Entity entityIn) {
        return false;
    }

    @Nonnull
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Nonnull
    public EnumBlockRenderType getRenderType(@Nonnull IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

}
