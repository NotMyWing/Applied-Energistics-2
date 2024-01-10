/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.util.helpers;

import appeng.api.util.AEColor;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class P2PHelperTest
{

	private P2PHelper unitUnderTest = new P2PHelper();

	private static final short WHITE_FREQUENCY = 0;
	private static final AEColor[] WHITE_COLORS = new AEColor[] { AEColor.WHITE, AEColor.WHITE, AEColor.WHITE, AEColor.WHITE };

	private static final short BLACK_FREQUENCY = (short) 0xFFFF;
	private static final AEColor[] BLACK_COLORS = new AEColor[] { AEColor.BLACK, AEColor.BLACK, AEColor.BLACK, AEColor.BLACK };

	private static final short MULTI_FREQUENCY = (short) 0xE8D1;
	private static final AEColor[] MULTI_COLORS = new AEColor[] { AEColor.RED, AEColor.LIGHT_GRAY, AEColor.GREEN, AEColor.ORANGE };

	private static final String HEX_WHITE_FREQUENCY = "0000";
	private static final String HEX_BLACK_FREQUENCY = "FFFF";
	private static final String HEX_MULTI_FREQUENCY = "E8D1";
	private static final String HEX_MIN_FREQUENCY = "8000";
	private static final String HEX_MAX_FREQUENCY = "7FFF";

	@Test
	public void testToColors()
	{
		assertThat( WHITE_COLORS, is( this.unitUnderTest.toColors( WHITE_FREQUENCY ) ) );
		assertThat( BLACK_COLORS, is( this.unitUnderTest.toColors( BLACK_FREQUENCY ) ) );
		assertThat( MULTI_COLORS, is( this.unitUnderTest.toColors( MULTI_FREQUENCY ) ) );
	}

	@Test
	public void testFromColors()
	{
		assertThat( WHITE_FREQUENCY, is( this.unitUnderTest.fromColors( WHITE_COLORS ) ) );
		assertThat( BLACK_FREQUENCY, is( this.unitUnderTest.fromColors( BLACK_COLORS ) ) );
		assertThat( MULTI_FREQUENCY, is( this.unitUnderTest.fromColors( MULTI_COLORS ) ) );
	}

	@Test
	public void testToAndFromColors()
	{
		for( short i = Short.MIN_VALUE; i < Short.MAX_VALUE; i++ )
		{
			assertThat( i, is( this.unitUnderTest.fromColors( this.unitUnderTest.toColors( i ) ) ));
		}
	}

	@Test
	public void testToHexDigit()
	{
		assertThat( "0", is( this.unitUnderTest.toHexDigit( AEColor.WHITE ) ) );
		assertThat( "1", is( this.unitUnderTest.toHexDigit( AEColor.ORANGE ) ) );
		assertThat( "2", is( this.unitUnderTest.toHexDigit( AEColor.MAGENTA ) ) );
		assertThat( "3", is( this.unitUnderTest.toHexDigit( AEColor.LIGHT_BLUE ) ) );
		assertThat( "4", is( this.unitUnderTest.toHexDigit( AEColor.YELLOW ) ) );
		assertThat( "5", is( this.unitUnderTest.toHexDigit( AEColor.LIME ) ) );
		assertThat( "6", is( this.unitUnderTest.toHexDigit( AEColor.PINK ) ) );
		assertThat( "7", is( this.unitUnderTest.toHexDigit( AEColor.GRAY ) ) );
		assertThat( "8", is( this.unitUnderTest.toHexDigit( AEColor.LIGHT_GRAY ) ) );
		assertThat( "9", is( this.unitUnderTest.toHexDigit( AEColor.CYAN ) ) );
		assertThat( "A", is( this.unitUnderTest.toHexDigit( AEColor.PURPLE ) ) );
		assertThat( "B", is( this.unitUnderTest.toHexDigit( AEColor.BLUE ) ) );
		assertThat( "C", is( this.unitUnderTest.toHexDigit( AEColor.BROWN ) ) );
		assertThat( "D", is( this.unitUnderTest.toHexDigit( AEColor.GREEN ) ) );
		assertThat( "E", is( this.unitUnderTest.toHexDigit( AEColor.RED ) ) );
		assertThat( "F", is( this.unitUnderTest.toHexDigit( AEColor.BLACK ) ) );
	}

	@Test
	public void testToHexString()
	{
		assertThat( HEX_WHITE_FREQUENCY, is( this.unitUnderTest.toHexString( WHITE_FREQUENCY ) ) );
		assertThat( HEX_BLACK_FREQUENCY, is( this.unitUnderTest.toHexString( BLACK_FREQUENCY ) ) );
		assertThat( HEX_MULTI_FREQUENCY, is( this.unitUnderTest.toHexString( MULTI_FREQUENCY ) ) );

		assertThat( HEX_MIN_FREQUENCY, is( this.unitUnderTest.toHexString( Short.MIN_VALUE ) ) );
		assertThat( HEX_MAX_FREQUENCY, is( this.unitUnderTest.toHexString( Short.MAX_VALUE ) ) );
	}
}
