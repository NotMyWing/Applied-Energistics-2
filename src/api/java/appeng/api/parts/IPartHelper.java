/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 AlgorithmX2
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.parts;


import appeng.api.util.AEPartLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;


public interface IPartHelper
{

	/**
	 * use in use item, to try and place a IBusItem
	 *
	 * @param is ItemStack of an item which implements {@link IPartItem}
	 * @param pos pos of part
	 * @param side side which the part should be on
	 * @param player player placing part
	 * @param world part in world
	 *
	 * @return true if placing was successful
	 */
	EnumActionResult placeBus( ItemStack is, BlockPos pos, EnumFacing side, EntityPlayer player, EnumHand hand, World world );

	/**
	 * @return the render mode
	 */
	CableRenderMode getCableRenderMode();

	/**
	 * Try to get a part at a specific place in the world.
	 *
	 * @param w    world the part is in
	 * @param pos  pos of the part host
	 * @param side side to test for a part on.
	 *
	 * @return the part if it exists, null otherwise
	 */
	@Nullable
	IPart getPart( World w, BlockPos pos, AEPartLocation side );

	/**
	 * Try to get a part host at a specific place in the world.
	 *
	 * @param w   world the part host is in
	 * @param pos pos of the part host
	 *
	 * @return the part host if it exists, null otherwise
	 */
	@Nullable
	IPartHost getPartHost( World w, BlockPos pos );

	/**
	 * Get a part host if it exists, or place one if it doesn't.
	 *
	 * @param w     world the part host is in
	 * @param pos   pos to get or place the part host
	 * @param force whether to skip permission and existing block checks and forcibly place the part host here.
	 * @param p     the placing player, or null if none
	 *
	 * @return the existing or created part host, or null if it didn't already exist and part host is unable to be placed.
	 */
	@Nullable
	IPartHost getOrPlacePartHost( World w, BlockPos pos, boolean force, @Nullable EntityPlayer p );

	/**
	 * Test if a part host can be successfully placed at a given position by the provided player.
	 *
	 * @param w   world the part host is in
	 * @param pos pos to test for part host placement
	 * @param p   the placing player, or null if none
	 *
	 * @return if the part can be placed at the provided world and position
	 */
	boolean canPlacePartHost( World w, BlockPos pos, @Nullable EntityPlayer p );
}
