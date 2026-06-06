package github.kasuminova.ecoaeextension.common.block.ecotech.estorage;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;

import javax.annotation.Nonnull;


@SuppressWarnings("deprecation")
public abstract class BlockEStoragePart extends BlockContainer {

    protected String harvestTool = "pickaxe";
    protected int harvestLevel = 0;

    protected BlockEStoragePart(final Material materialIn) {
        super(materialIn);
        this.translucent = true;
        this.fullBlock = false;
        this.lightOpacity = 0;
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
    public boolean hasTileEntity() {
        return true;
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
