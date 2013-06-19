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

package net.nikr.eve.jeveasset.gui.tabs.tree;

import ca.odell.glazedlists.swing.DefaultEventTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.table.TableCellRenderer;
import net.nikr.eve.jeveasset.Program;
import net.nikr.eve.jeveasset.gui.shared.table.JAutoColumnTable;


public class JTreeTable extends JAutoColumnTable {

	private DefaultEventTableModel<TreeAsset> tableModel;
	private Color color = new Color(240, 240, 240);

	public JTreeTable(final Program program, final DefaultEventTableModel<TreeAsset> tableModel) {
		super(program, tableModel);
		this.tableModel = tableModel;

		//setShowHorizontalLines(false);
		//setShowVerticalLines(true);
		//setGridColor(new Color(200, 200, 200));
		//setShowGrid(false);
		setCellSelectionEnabled(true);
		//setRowSelectionAllowed(true);
		//setColumnSelectionAllowed(false);
		//FIXME - - > TreeTable: Reordering columns does not work
		getTableHeader().setReorderingAllowed(false);
		setRowHeight(20);
		//setRowMargin(0);
		//setIntercellSpacing(new Dimension(1, 0));
	}

	@Override
	public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {
		Component component = super.prepareRenderer(renderer, row, column);
		boolean isSelected = isCellSelected(row, column);
		TreeAsset treeAsset = tableModel.getElementAt(row);
		if (!isSelected) {
			if (treeAsset.isTrueAsset()) {
				component.setBackground(Color.WHITE);
			} else if (treeAsset.getDepth() == 1) {
				component.setBackground(new Color(190, 190, 190));
			} else if (treeAsset.getDepth() == 2) {
				component.setBackground(new Color(210, 210, 210));
			} else if (treeAsset.getDepth() >= 3) {
				component.setBackground(new Color(230, 230, 230));
			} else {
				component.setBackground(Color.WHITE);
			}
		}
		return component;
	}
}
