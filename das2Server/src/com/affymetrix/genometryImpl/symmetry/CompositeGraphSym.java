// 
// Decompiled by Procyon v0.5.30
// 

package com.affymetrix.genometryImpl.symmetry;

import java.util.Arrays;
import com.affymetrix.genometryImpl.BioSeq;

public final class CompositeGraphSym extends GraphSym
{
    public CompositeGraphSym(final String id, final BioSeq seq) {
        super(null, null, id, seq);
    }
    
    @Override
    public void addChild(final SeqSymmetry sym) {
        if (!(sym instanceof GraphSym)) {
            throw new RuntimeException("only GraphSyms can be added as children to CompositeGraphSym!");
        }
        final GraphSym slice = (GraphSym)sym;
        if (slice.getPointCount() > 0) {
            if (this.getPointCount() == 0) {
                final int[] slice_xcoords = slice.getGraphXCoords();
                final float[] slice_ycoords = slice.getGraphYCoords();
                final int[] slice_wcoords = slice.getGraphWidthCoords();
                slice.nullCoords();
                this.setCoords(slice_xcoords, slice_ycoords, slice_wcoords);
            }
            else {
                this.createNewCoords(slice);
            }
        }
    }
    
    private void createNewCoords(final GraphSym slice) {
        final int slice_min = slice.getMinXCoord();
        final int slice_index = this.determineBegIndex(slice_min);
        final int coordSize = this.getPointCount();
        final int sliceSize = slice.getPointCount();
        final int[] slice_xcoords = slice.getGraphXCoords();
        final float[] slice_ycoords = slice.getGraphYCoords();
        final int[] slice_wcoords = slice.getGraphWidthCoords();
        slice.nullCoords();
        final int[] old_xcoords = this.getGraphXCoords();
        final int[] new_xcoords = copyIntCoords(coordSize, sliceSize, slice_index, old_xcoords, slice_xcoords);
        final float[] old_ycoords = this.getGraphYCoords();
        final float[] new_ycoords = copyFloatCoords(coordSize, sliceSize, slice_index, old_ycoords, slice_ycoords);
        final int[] old_wcoords = this.getGraphWidthCoords();
        final int[] new_wcoords = (int[])((old_wcoords == null) ? null : copyIntCoords(coordSize, sliceSize, slice_index, old_wcoords, slice_wcoords));
        this.setCoords(new_xcoords, new_ycoords, new_wcoords);
    }
    
    private static int[] copyIntCoords(final int coordSize, final int sliceSize, final int slice_index, final int[] old_coords, final int[] slice_coords) {
        final int[] new_coords = new int[coordSize + sliceSize];
        int new_index = 0;
        if (old_coords == null) {
            Arrays.fill(new_coords, new_index, new_index + slice_index, 0);
        }
        else {
            System.arraycopy(old_coords, 0, new_coords, new_index, slice_index);
        }
        new_index += slice_index;
        if (slice_coords == null) {
            Arrays.fill(new_coords, new_index, new_index + sliceSize, 0);
        }
        else {
            System.arraycopy(slice_coords, 0, new_coords, new_index, sliceSize);
        }
        new_index += sliceSize;
        if (old_coords == null) {
            Arrays.fill(new_coords, new_index, sliceSize + coordSize, 0);
        }
        else {
            System.arraycopy(old_coords, slice_index, new_coords, new_index, coordSize - slice_index);
        }
        return new_coords;
    }
    
    private static float[] copyFloatCoords(final int coordSize, final int sliceSize, final int slice_index, final float[] old_coords, final float[] slice_coords) {
        final float[] new_coords = new float[coordSize + sliceSize];
        int new_index = 0;
        System.arraycopy(old_coords, 0, new_coords, new_index, slice_index);
        new_index += slice_index;
        System.arraycopy(slice_coords, 0, new_coords, new_index, sliceSize);
        new_index += sliceSize;
        System.arraycopy(old_coords, slice_index, new_coords, new_index, coordSize - slice_index);
        return new_coords;
    }
}
