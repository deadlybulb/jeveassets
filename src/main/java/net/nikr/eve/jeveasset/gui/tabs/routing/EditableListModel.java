/*
 * Copyright 2009-2013 Contributors (see credits.txt)
 *
 * This file is part of jEveAssets.
 *
 * jEveAssets is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * jEveAssets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jEveAssets; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package net.nikr.eve.jeveasset.gui.tabs.routing;

import java.util.*;
import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;

public class EditableListModel<T> extends AbstractListModel {

	private static final long serialVersionUID = 1L;
	private List<T> backed = new ArrayList<T>();
	private Comparator<T> sortComparator = new Comparator<T>() {
		@Override
		public int compare(final T o1, final T o2) {
			return o2.hashCode() - o1.hashCode();
		}
	};

	public EditableListModel() {
	}

	public EditableListModel(final List<T> initial) {
		backed.addAll(initial);
	}

	public EditableListModel(final List<T> initial, final Comparator<T> sortComparator) {
		backed.addAll(initial);
		setSortComparator(sortComparator);
	}

	public final void setSortComparator(final Comparator<T> sortComparator) {
		this.sortComparator = sortComparator;
		Collections.sort(backed, sortComparator);
	}

	public Comparator<T> getSortComparator() {
		return sortComparator;
	}

	public List<? extends T> getAll() {
		return Collections.unmodifiableList(backed);
	}

	@Override
	public int getSize() {
		return backed.size();
	}

	@Override
	public Object getElementAt(final int index) {
		return backed.get(index);
	}

	public T remove(final int index) {
		T b = backed.remove(index);
		changed();
		return b;
	}

	public void clear() {
		backed.clear();
		changed();
	}

	public boolean remove(final T o) {
		boolean b = backed.remove(o);
		changed();
		return b;
	}

	public boolean add(final T e) {
		boolean b = backed.add(e);
		Collections.sort(backed, sortComparator);
		changed();
		return b;
	}

	public boolean addAll(final Collection<? extends T> c) {
		boolean b = backed.addAll(c);
		Collections.sort(backed, sortComparator);
		changed();
		return b;
	}

	public boolean contains(final T o) {
		return backed.contains(o);
	}

	void changed() {
		if (SwingUtilities.isEventDispatchThread()) {
			fireContentsChanged(this, 0, backed.size() - 1);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					fireContentsChanged(this, 0, backed.size() - 1);
				}
			});
		}
	}
}
