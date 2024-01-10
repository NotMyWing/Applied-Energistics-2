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

package appeng.services.version;


import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 * Tests for {@link Version}
 *
 * @author thatsIch
 * @version rv3 - 16.05.2015
 * @since rv3 16.05.2015
 */
public final class VersionTest
{
	private static final Version DEFAULT_VERSION_RV2_BETA_8 = new DefaultVersion( 2, Channel.Beta, 8 );
	private static final Version DEFAULT_VERSION_RV2_BETA_9 = new DefaultVersion( 2, Channel.Beta, 9 );
	private static final Version DEFAULT_VERSION_RV3_BETA_8 = new DefaultVersion( 3, Channel.Beta, 8 );
	private static final Version DEFAULT_VERSION_RV2_ALPHA_8 = new DefaultVersion( 2, Channel.Alpha, 8 );
	private static final Version DEFAULT_VERSION_RV4_ALPHA_1 = new DefaultVersion( 4, Channel.Alpha, 1 );
	private static final Version DO_NOT_CHECK_VERSION = new DoNotCheckVersion();
	private static final Version MISSING_VERSION = new MissingVersion();

	@Test
	public void testDevBuild()
	{
		assertThat( DO_NOT_CHECK_VERSION.formatted(), is( "dev build" ) );
	}

	@Test
	public void testMissingBuild()
	{
		assertThat( MISSING_VERSION.formatted(), is ( "missing" ) );
	}

	@Test
	public void compareVersionToDoNotCheck()
	{
		assertThat( DEFAULT_VERSION_RV2_ALPHA_8.isNewerAs( DO_NOT_CHECK_VERSION ), is( false ) );
		assertThat( DO_NOT_CHECK_VERSION.isNewerAs( DEFAULT_VERSION_RV2_ALPHA_8 ), is( true ) );
	}

	@Test
	public void compareVersionToMissingVersion()
	{
		assertThat( DEFAULT_VERSION_RV2_ALPHA_8.isNewerAs( MISSING_VERSION ), is( true ) );
		assertThat( MISSING_VERSION.isNewerAs( DEFAULT_VERSION_RV2_ALPHA_8 ), is( false ) );
	}

	@Test
	public void compareTwoDefaultVersions()
	{
		assertThat( DEFAULT_VERSION_RV2_BETA_8.isNewerAs( DEFAULT_VERSION_RV2_ALPHA_8 ), is( true ) );
		assertThat( DEFAULT_VERSION_RV4_ALPHA_1.isNewerAs( DEFAULT_VERSION_RV3_BETA_8 ), is( true ) );
		assertThat( DEFAULT_VERSION_RV2_BETA_9.isNewerAs( DEFAULT_VERSION_RV2_BETA_8 ), is( true ) );
	}

	@Test
	public void testEqualsNonVersion()
	{
		assertThat( DEFAULT_VERSION_RV2_ALPHA_8, is( not( equalTo( new Object() ) ) ) );
	}

	@Test
	public void testEqualsUnequalBuild()
	{
		assertThat( DEFAULT_VERSION_RV2_BETA_8, is( not( equalTo( DEFAULT_VERSION_RV2_BETA_9 ) ) ) );
	}

	@Test
	public void testEqualsUnequalChannel()
	{
		assertThat( DEFAULT_VERSION_RV2_BETA_8, is( not( equalTo( DEFAULT_VERSION_RV2_ALPHA_8 ) ) ) );
	}

	@Test
	public void testEqualsUnequalRevision()
	{
		assertThat( DEFAULT_VERSION_RV2_BETA_8, is( not( equalTo( DEFAULT_VERSION_RV3_BETA_8 ) ) ) );
	}

	@Test
	public void testUnequalHash()
	{
		assertThat( DEFAULT_VERSION_RV2_BETA_8.hashCode(), is( not( equalTo( DEFAULT_VERSION_RV2_ALPHA_8.hashCode() ) ) ) );
	}

	@Test
	public void testToString()
	{
		assertThat( DEFAULT_VERSION_RV2_BETA_8.toString(), is( "Version{revision=2, channel=Beta, build=8}" ) );
	}

	@Test
	public void testFormatted()
	{
		assertThat( DEFAULT_VERSION_RV2_BETA_8.formatted(), is( "rv2-beta-8" ) );
	}

}
