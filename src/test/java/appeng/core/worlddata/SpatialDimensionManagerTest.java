
package appeng.core.worlddata;


import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class SpatialDimensionManagerTest
{
	private static final BlockPos CONTENT = new BlockPos( 1, 1, 1 );

	private static final int ID_1 = 0;
	private static final int ID_2 = 1;
	private static final int ID_3 = 2;
	private static final int ID_4 = 3;
	private static final int ID_5 = 4;
	private static final int ID_6 = 5;
	private static final int ID_7 = 6;
	private static final int ID_8 = 7;
	private static final int ID_9 = 8;
	private static final int ID_A = 9;
	private static final int ID_B = 10;
	private static final int ID_C = 11;

	private static final BlockPos POS_1 = new BlockPos( -320, 64, -320 );
	private static final BlockPos POS_2 = new BlockPos( 192, 64, -320 );
	private static final BlockPos POS_3 = new BlockPos( -320, 64, 192 );
	private static final BlockPos POS_4 = new BlockPos( 192, 64, 192 );

	private static final BlockPos POS_5 = new BlockPos( -832, 64, -320 );
	private static final BlockPos POS_6 = new BlockPos( 704, 64, -320 );
	private static final BlockPos POS_7 = new BlockPos( -832, 64, 192 );
	private static final BlockPos POS_8 = new BlockPos( 704, 64, 192 );

	private static final BlockPos POS_9 = new BlockPos( -320, 64, -832 );
	private static final BlockPos POS_A = new BlockPos( 192, 64, -832 );
	private static final BlockPos POS_B = new BlockPos( -320, 64, 704 );
	private static final BlockPos POS_C = new BlockPos( 192, 64, 704 );

	@Test
	public void testBlockPosGeneration()
	{
		SpatialDimensionManager manager = new SpatialDimensionManager( null );

		assertThat( ID_1, is(manager.createNewCellDimension( CONTENT, -1 ) ) );
		assertThat( ID_2, is(manager.createNewCellDimension( CONTENT, -1 ) ) );
		assertThat( ID_3, is(manager.createNewCellDimension( CONTENT, -1 ) ) );
		assertThat( ID_4, is(manager.createNewCellDimension( CONTENT, -1 ) ) );
		assertThat( ID_5, is(manager.createNewCellDimension( CONTENT, -1 ) ) );
		assertThat( ID_6, is(manager.createNewCellDimension( CONTENT, -1 ) ) );
		assertThat( ID_7, is(manager.createNewCellDimension( CONTENT, -1 ) ) );
		assertThat( ID_8, is(manager.createNewCellDimension( CONTENT, -1 ) ) );
		assertThat( ID_9, is(manager.createNewCellDimension( CONTENT, -1 ) ) );
		assertThat( ID_A, is(manager.createNewCellDimension( CONTENT, -1 ) ) );
		assertThat( ID_B, is(manager.createNewCellDimension( CONTENT, -1 ) ) );
		assertThat( ID_C, is(manager.createNewCellDimension( CONTENT, -1 ) ) );

		assertThat( POS_1, is(manager.getCellDimensionOrigin( ID_1 ) ) );
		assertThat( POS_2, is(manager.getCellDimensionOrigin( ID_2 ) ) );
		assertThat( POS_3, is(manager.getCellDimensionOrigin( ID_3 ) ) );
		assertThat( POS_4, is(manager.getCellDimensionOrigin( ID_4 ) ) );
		assertThat( POS_5, is(manager.getCellDimensionOrigin( ID_5 ) ) );
		assertThat( POS_6, is(manager.getCellDimensionOrigin( ID_6 ) ) );
		assertThat( POS_7, is(manager.getCellDimensionOrigin( ID_7 ) ) );
		assertThat( POS_8, is(manager.getCellDimensionOrigin( ID_8 ) ) );
		assertThat( POS_9, is(manager.getCellDimensionOrigin( ID_9 ) ) );
		assertThat( POS_A, is(manager.getCellDimensionOrigin( ID_A ) ) );
		assertThat( POS_B, is(manager.getCellDimensionOrigin( ID_B ) ) );
		assertThat( POS_C, is(manager.getCellDimensionOrigin( ID_C ) ) );
	}

}
