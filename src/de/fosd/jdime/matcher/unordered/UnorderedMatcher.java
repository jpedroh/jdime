/*
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 */
package de.fosd.jdime.matcher.unordered;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.MatchingInterface;
import de.fosd.jdime.matcher.Matchings;
import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

/**
 * <code>UnorderedMatcher</code>s ignore the order of the elements they match when comparing <code>Artifact</code>s.
 *
 * @param <T>
 * 		the type of the <code>Artifact</code>s
 * @author Olaf Lessenich
 */
public abstract class UnorderedMatcher<T extends Artifact<T>> implements MatchingInterface<T> {

	protected static final Logger LOG = Logger.getLogger(ClassUtils.getShortClassName(Matcher.class));

	/**
	 * The matcher is used for recursive matching calls. It can determine whether the order of artifacts is essential.
	 */
	Matcher<T> matcher;

	/**
	 * Constructs a new <code>UnorderedMatcher</code> using the given <code>matcher</code> for recursive calls.
	 *
	 * @param matcher
	 * 		the parent <code>Matcher</code>
	 */
	public UnorderedMatcher(final Matcher<T> matcher) {
		this.matcher = matcher;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Compares <code>left</code> and <code>right</code> while ignoring the order of the elements.
	 */
	@Override
	public abstract Matchings<T> match(MergeContext context, T left, T right, int lookAhead);
}
