/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.parts;


import appeng.api.parts.IPartCollisionHelper;
import appeng.api.util.AEPartLocation;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;


public class BusCollisionHelper implements IPartCollisionHelper {

    private final List<AxisAlignedBB> boxes;

    private final EnumFacing x;
    private final EnumFacing y;
    private final EnumFacing z;

    private final Entity entity;
    private final boolean isVisual;

    public BusCollisionHelper(final List<AxisAlignedBB> boxes, final EnumFacing x, final EnumFacing y, final EnumFacing z, final Entity e, final boolean visual) {
        this.boxes = boxes;
        this.x = x;
        this.y = y;
        this.z = z;
        this.entity = e;
        this.isVisual = visual;
    }

    public BusCollisionHelper(final List<AxisAlignedBB> boxes, final AEPartLocation s, final Entity e, final boolean visual) {
        this.boxes = boxes;
        this.entity = e;
        this.isVisual = visual;

        switch (s) {
            case DOWN:
                this.x = EnumFacing.EAST;
                this.y = EnumFacing.NORTH;
                this.z = EnumFacing.DOWN;
                break;
            case UP:
                this.x = EnumFacing.EAST;
                this.y = EnumFacing.SOUTH;
                this.z = EnumFacing.UP;
                break;
            case EAST:
                this.x = EnumFacing.SOUTH;
                this.y = EnumFacing.UP;
                this.z = EnumFacing.EAST;
                break;
            case WEST:
                this.x = EnumFacing.NORTH;
                this.y = EnumFacing.UP;
                this.z = EnumFacing.WEST;
                break;
            case NORTH:
                this.x = EnumFacing.WEST;
                this.y = EnumFacing.UP;
                this.z = EnumFacing.NORTH;
                break;
            case SOUTH:
                this.x = EnumFacing.EAST;
                this.y = EnumFacing.UP;
                this.z = EnumFacing.SOUTH;
                break;
            case INTERNAL:
            default:
                this.x = EnumFacing.EAST;
                this.y = EnumFacing.UP;
                this.z = EnumFacing.SOUTH;
                break;
        }
    }

    /**
     * pretty much useless...
     */
    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public void addBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        minX /= 16.0;
        minY /= 16.0;
        minZ /= 16.0;
        maxX /= 16.0;
        maxY /= 16.0;
        maxZ /= 16.0;

        double aX = minX * this.x.getXOffset() + minY * this.y.getXOffset() + minZ * this.z.getXOffset();
        double aY = minX * this.x.getYOffset() + minY * this.y.getYOffset() + minZ * this.z.getYOffset();
        double aZ = minX * this.x.getZOffset() + minY * this.y.getZOffset() + minZ * this.z.getZOffset();

        double bX = maxX * this.x.getXOffset() + maxY * this.y.getXOffset() + maxZ * this.z.getXOffset();
        double bY = maxX * this.x.getYOffset() + maxY * this.y.getYOffset() + maxZ * this.z.getYOffset();
        double bZ = maxX * this.x.getZOffset() + maxY * this.y.getZOffset() + maxZ * this.z.getZOffset();

        if (this.x.getXOffset() + this.y.getXOffset() + this.z.getXOffset() < 0) {
            aX += 1;
            bX += 1;
        }

        if (this.x.getYOffset() + this.y.getYOffset() + this.z.getYOffset() < 0) {
            aY += 1;
            bY += 1;
        }

        if (this.x.getZOffset() + this.y.getZOffset() + this.z.getZOffset() < 0) {
            aZ += 1;
            bZ += 1;
        }

        minX = Math.min(aX, bX);
        minY = Math.min(aY, bY);
        minZ = Math.min(aZ, bZ);
        maxX = Math.max(aX, bX);
        maxY = Math.max(aY, bY);
        maxZ = Math.max(aZ, bZ);

        this.boxes.add(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    @Override
    public EnumFacing getWorldX() {
        return this.x;
    }

    @Override
    public EnumFacing getWorldY() {
        return this.y;
    }

    @Override
    public EnumFacing getWorldZ() {
        return this.z;
    }

    @Override
    public boolean isBBCollision() {
        return !this.isVisual;
    }
}
