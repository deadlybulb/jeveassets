/*
 * Copyright 2009-2016 Contributors (see credits.txt)
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

package net.nikr.eve.jeveasset.gui.tabs.loadout;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.SeparatorList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import net.nikr.eve.jeveasset.Program;
import net.nikr.eve.jeveasset.data.EventListManager;
import net.nikr.eve.jeveasset.data.Item;
import net.nikr.eve.jeveasset.data.Settings;
import net.nikr.eve.jeveasset.gui.images.Images;
import net.nikr.eve.jeveasset.gui.shared.CaseInsensitiveComparator;
import net.nikr.eve.jeveasset.gui.shared.components.JCustomFileChooser;
import net.nikr.eve.jeveasset.gui.shared.components.JDropDownButton;
import net.nikr.eve.jeveasset.gui.shared.components.JFixedToolBar;
import net.nikr.eve.jeveasset.gui.shared.components.JMainTab;
import net.nikr.eve.jeveasset.gui.shared.components.ListComboBoxModel;
import net.nikr.eve.jeveasset.gui.shared.filter.ExportDialog;
import net.nikr.eve.jeveasset.gui.shared.filter.ExportFilterControl;
import net.nikr.eve.jeveasset.gui.shared.menu.*;
import net.nikr.eve.jeveasset.gui.shared.menu.MenuManager.TableMenu;
import net.nikr.eve.jeveasset.gui.shared.table.EnumTableColumn;
import net.nikr.eve.jeveasset.gui.shared.table.EnumTableFormatAdaptor;
import net.nikr.eve.jeveasset.gui.shared.table.EventModels;
import net.nikr.eve.jeveasset.gui.shared.table.JSeparatorTable;
import net.nikr.eve.jeveasset.gui.shared.table.PaddingTableCellRenderer;
import net.nikr.eve.jeveasset.gui.tabs.assets.MyAsset;
import net.nikr.eve.jeveasset.gui.tabs.loadout.Loadout.FlagType;
import net.nikr.eve.jeveasset.i18n.General;
import net.nikr.eve.jeveasset.i18n.GuiShared;
import net.nikr.eve.jeveasset.i18n.TabsLoadout;
import net.nikr.eve.jeveasset.io.local.EveFittingWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LoadoutsTab extends JMainTab {

	private static final Logger LOG = LoggerFactory.getLogger(LoadoutsTab.class);

	private enum LoadoutsAction {
		FILTER,
		OWNERS,
		EXPORT,
		EXPORT_LOADOUT_SELECTED,
		EXPORT_LOADOUT_ALL,
		COLLAPSE,
		EXPAND
	}

	private static final String SHIP_CATEGORY = "Ship";

	//GUI
	private final JComboBox<String> jOwners;
	private final JComboBox<String> jShips;
	private final JButton jExpand;
	private final JButton jCollapse;
	private final JSeparatorTable jTable;
	private final JDropDownButton jExport;
	private final LoadoutsExportDialog loadoutsExportDialog;
	private JCustomFileChooser jXmlFileChooser;

	//Table
	private final EventList<Loadout> eventList;
	private final FilterList<Loadout> filterList;
	private final SeparatorList<Loadout> separatorList;
	private final DefaultEventSelectionModel<Loadout> selectionModel;
	private final DefaultEventTableModel<Loadout> tableModel;
	private final EnumTableFormatAdaptor<LoadoutTableFormat, Loadout> tableFormat;

	//Dialog
	ExportDialog<Loadout> exportDialog;

	public static final String NAME = "loadouts"; //Not to be changed!

	public LoadoutsTab(final Program program) {
		super(program, TabsLoadout.get().ship(), Images.TOOL_SHIP_LOADOUTS.getIcon(), true);

		loadoutsExportDialog = new LoadoutsExportDialog(program, this);

		ListenerClass listener = new ListenerClass();

		try {
			jXmlFileChooser = new JCustomFileChooser(program.getMainWindow().getFrame(), "xml");
		} catch (RuntimeException e) {
			// Workaround for JRE bug 4711700. A NullPointer is thrown
			// sometimes on the first construction under XP look and feel,
			// but construction succeeds on successive attempts.
			try {
				jXmlFileChooser = new JCustomFileChooser(program.getMainWindow().getFrame(), "xml");
			} catch (RuntimeException npe) {
				// ok, now we use the metal file chooser, takes a long time to load
				// but the user can still use the program
				UIManager.getDefaults().put("FileChooserUI", "javax.swing.plaf.metal.MetalFileChooserUI");
				jXmlFileChooser = new JCustomFileChooser(program.getMainWindow().getFrame(), "xml");
			}
		}

		JFixedToolBar jToolBarTop = new JFixedToolBar();

		JLabel jOwnersLabel = new JLabel(TabsLoadout.get().owner());
		jToolBarTop.add(jOwnersLabel);

		jOwners = new JComboBox<String>();
		jOwners.setActionCommand(LoadoutsAction.OWNERS.name());
		jOwners.addActionListener(listener);
		jToolBarTop.addComboBox(jOwners, 200);

		JLabel jShipsLabel = new JLabel(TabsLoadout.get().ship1());
		jToolBarTop.add(jShipsLabel);

		jShips = new JComboBox<String>();
		jShips.setActionCommand(LoadoutsAction.FILTER.name());
		jShips.addActionListener(listener);
		jToolBarTop.addComboBox(jShips, 0);

		JFixedToolBar jToolBarLeft = new JFixedToolBar();

		jExport = new JDropDownButton(GuiShared.get().export(), Images.DIALOG_CSV_EXPORT.getIcon());
		jToolBarLeft.addButton(jExport);

		JMenuItem jExportSqlCsvHtml = new JMenuItem(TabsLoadout.get().exportTableData(), Images.DIALOG_CSV_EXPORT.getIcon());
		jExportSqlCsvHtml.setActionCommand(LoadoutsAction.EXPORT.name());
		jExportSqlCsvHtml.addActionListener(listener);
		jExport.add(jExportSqlCsvHtml);

		JMenu jMenu = new JMenu(TabsLoadout.get().exportEveXml());
		jMenu.setIcon(Images.MISC_EVE.getIcon());
		jExport.add(jMenu);
		
		JMenuItem jExportEveXml = new JMenuItem(TabsLoadout.get().exportEveXmlSelected());
		jExportEveXml.setActionCommand(LoadoutsAction.EXPORT_LOADOUT_SELECTED.name());
		jExportEveXml.addActionListener(listener);
		jMenu.add(jExportEveXml);

		JMenuItem jExportEveXmlAll = new JMenuItem(TabsLoadout.get().exportEveXmlAll());
		jExportEveXmlAll.setActionCommand(LoadoutsAction.EXPORT_LOADOUT_ALL.name());
		jExportEveXmlAll.addActionListener(listener);
		jMenu.add(jExportEveXmlAll);

		JFixedToolBar jToolBarRight = new JFixedToolBar();

		jCollapse = new JButton(TabsLoadout.get().collapse(), Images.MISC_COLLAPSED.getIcon());
		jCollapse.setActionCommand(LoadoutsAction.COLLAPSE.name());
		jCollapse.addActionListener(listener);
		jToolBarRight.addButton(jCollapse);

		jExpand = new JButton(TabsLoadout.get().expand(), Images.MISC_EXPANDED.getIcon());
		jExpand.setActionCommand(LoadoutsAction.EXPAND.name());
		jExpand.addActionListener(listener);
		jToolBarRight.addButton(jExpand);

		//Table Format
		tableFormat = new EnumTableFormatAdaptor<LoadoutTableFormat, Loadout>(LoadoutTableFormat.class);
		//Backend
		eventList = new EventListManager<Loadout>().create();
		//Filter
		eventList.getReadWriteLock().readLock().lock();
		filterList = new FilterList<Loadout>(eventList);
		eventList.getReadWriteLock().readLock().unlock();
		//Separator
		separatorList = new SeparatorList<Loadout>(filterList, new LoadoutSeparatorComparator(), 1, Integer.MAX_VALUE);
		//Table Model
		tableModel = EventModels.createTableModel(separatorList, tableFormat);
		//Table
		jTable = new JSeparatorTable(program, tableModel, separatorList);
		jTable.setSeparatorRenderer(new LoadoutSeparatorTableCell(jTable, separatorList));
		jTable.setSeparatorEditor(new LoadoutSeparatorTableCell(jTable, separatorList));
		PaddingTableCellRenderer.install(jTable, 3);
		//Selection Model
		selectionModel = EventModels.createSelectionModel(separatorList);
		selectionModel.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
		jTable.setSelectionModel(selectionModel);
		//Listeners
		installTable(jTable, NAME);
		//Scroll
		JScrollPane jTableScroll = new JScrollPane(jTable);
		//Menu
		installMenu(program, new LoadoutTableMenu(), jTable, Loadout.class);

		List<EnumTableColumn<Loadout>> enumColumns = new ArrayList<EnumTableColumn<Loadout>>();
		enumColumns.addAll(Arrays.asList(LoadoutExtendedTableFormat.values()));
		enumColumns.addAll(Arrays.asList(LoadoutTableFormat.values()));
		List<EventList<Loadout>> eventLists = new ArrayList<EventList<Loadout>>();
		eventLists.add(filterList);
		exportDialog = new ExportDialog<Loadout>(program.getMainWindow().getFrame(), NAME, null, new LoadoutsFilterControl(), eventLists, enumColumns);

		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(jToolBarTop, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Integer.MAX_VALUE)
				.addGroup(layout.createSequentialGroup()
					.addComponent(jToolBarLeft, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Integer.MAX_VALUE)
					.addGap(0)
					.addComponent(jToolBarRight)
				)
				.addComponent(jTableScroll, 0, 0, Short.MAX_VALUE)
		);
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				.addComponent(jToolBarTop, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(jToolBarLeft, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(jToolBarRight, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				)
				.addComponent(jTableScroll, 0, 0, Short.MAX_VALUE)
		);
	}

	@Override
	public void updateData() {
		if (!program.getOwnerNames(false).isEmpty()) {
			jOwners.setEnabled(true);
			String selectedItem = (String) jOwners.getSelectedItem();
			jOwners.setModel(new ListComboBoxModel<String>(program.getOwnerNames(true)));
			if (selectedItem != null && program.getOwnerNames(true).contains(selectedItem)) {
				jOwners.setSelectedItem(selectedItem);
			} else {
				jOwners.setSelectedIndex(0);
			}
		} else {
			jOwners.setEnabled(false);
			jOwners.setModel(new ListComboBoxModel<String>());
			jOwners.getModel().setSelectedItem(TabsLoadout.get().no());
			jShips.setModel(new ListComboBoxModel<String>());
			jShips.getModel().setSelectedItem(TabsLoadout.get().no());
		}
		updateTable();
	}

	private String browse() {
		File windows = new File(javax.swing.filechooser.FileSystemView.getFileSystemView().getDefaultDirectory()
							+ File.separator + "EVE"
							+ File.separator + "fittings"
							);
		File mac = new File(System.getProperty("user.home", ".")
							+ File.separator + "Library"
							+ File.separator + "Preferences"
							+ File.separator + "EVE Online Preferences"
							+ File.separator + "p_drive"
							+ File.separator + "My Documents"
							+ File.separator + "EVE"
							+ File.separator + "fittings"
							);
		LOG.info("Mac Browsing: {}", mac.getAbsolutePath());
		if (windows.exists()) { //Windows
			jXmlFileChooser.setCurrentDirectory(windows);
		} else if (mac.exists()) { //Mac
			//PENDING TEST if fittings path is set correct on mac
			//			should open: ~library/preferences/eve online preferences/p_drive/my documents/eve/overview
			jXmlFileChooser.setCurrentDirectory(mac);
		} else { //Others: use program directory is there is only Win & Mac clients
			jXmlFileChooser.setCurrentDirectory(new File(Settings.getUserDirectory()));
		}
		int bFound = jXmlFileChooser.showSaveDialog(program.getMainWindow().getFrame());
		if (bFound  == JFileChooser.APPROVE_OPTION) {
			File file = jXmlFileChooser.getSelectedFile();
			return file.getAbsolutePath();
		} else {
			return null;
		}
	}

	public void export() {
		String fitName = loadoutsExportDialog.getFittingName();
		String fitDescription = loadoutsExportDialog.getFittingDescription();
		if (!fitName.isEmpty()) {
			String selectedShip = (String) jShips.getSelectedItem();
			MyAsset exportAsset = null;
			for (MyAsset asset : program.getAssetList()) {
					String key = asset.getName() + " #" + asset.getItemID();
					if (selectedShip.equals(key)) {
						exportAsset = asset;
						break;
					}
				}
			loadoutsExportDialog.setVisible(false);
			if (exportAsset == null) {
				return;
			}
			String filename = browse();
			if (filename != null) {
				EveFittingWriter.save(Collections.singletonList(exportAsset), filename, fitName, fitDescription);
			}
		} else {
			JOptionPane.showMessageDialog(loadoutsExportDialog.getDialog(),
					TabsLoadout.get().name1(),
					TabsLoadout.get().empty(),
					JOptionPane.PLAIN_MESSAGE);
		}
	}

	private void updateTable() {
		List<Loadout> ship = new ArrayList<Loadout>();
		for (MyAsset asset : program.getAssetList()) {
			String key = asset.getName() + " #" + asset.getItemID();
			if (!asset.getItem().getCategory().equals(SHIP_CATEGORY) || !asset.isSingleton()) {
				continue;
			}
			Loadout moduleShip = new Loadout(asset.getItem(), asset.getLocation(), asset.getOwner(), TabsLoadout.get().totalShip(), key, TabsLoadout.get().flagTotalValue(), null, asset.getDynamicPrice(), 1);
			Loadout moduleModules = new Loadout(new Item(0), asset.getLocation(), asset.getOwner(), TabsLoadout.get().totalModules(), key, TabsLoadout.get().flagTotalValue(), null, 0, 0);
			Loadout moduleTotal = new Loadout(new Item(0), asset.getLocation(), asset.getOwner(), TabsLoadout.get().totalAll(), key, TabsLoadout.get().flagTotalValue(), null, asset.getDynamicPrice(), 1);
			ship.add(moduleShip);
			ship.add(moduleModules);
			ship.add(moduleTotal);
			for (MyAsset assetModule : asset.getAssets()) {
				Loadout module = new Loadout(assetModule.getItem(), assetModule.getLocation(), assetModule.getOwner(), assetModule.getName(), key, assetModule.getFlag(), assetModule.getDynamicPrice(), (assetModule.getDynamicPrice() * assetModule.getCount()), assetModule.getCount());
				if (!ship.contains(module)
						|| assetModule.getFlag().contains(FlagType.HIGH_SLOT.getFlag())
						|| assetModule.getFlag().contains(FlagType.MEDIUM_SLOT.getFlag())
						|| assetModule.getFlag().contains(FlagType.LOW_SLOT.getFlag())
						|| assetModule.getFlag().contains(FlagType.RIG_SLOTS.getFlag())
						|| assetModule.getFlag().contains(FlagType.SUB_SYSTEMS.getFlag())
						) {
					ship.add(module);
				} else {
					module = ship.get(ship.indexOf(module));
					module.addCount(assetModule.getCount());
					module.addValue(assetModule.getDynamicPrice() * assetModule.getCount());
				}
				moduleModules.addValue(assetModule.getDynamicPrice() * assetModule.getCount());
				moduleModules.addCount(assetModule.getCount());
				moduleTotal.addValue(assetModule.getDynamicPrice() * assetModule.getCount());
				moduleTotal.addCount(assetModule.getCount());
			}
		}
		Collections.sort(ship);
		String key = "";
		for (Loadout module : ship) {
			if (!key.equals(module.getKey())) {
				module.first();
				key = module.getKey();
			}
		}
		//Save separator expanded/collapsed state
		jTable.saveExpandedState();
		//Update list
		try {
			eventList.getReadWriteLock().writeLock().lock();
			eventList.clear();
			eventList.addAll(ship);
		} finally {
			eventList.getReadWriteLock().writeLock().unlock();
		}
		//Restore separator expanded/collapsed state
		jTable.loadExpandedState();
	}

	private class LoadoutTableMenu implements TableMenu<Loadout> {
		@Override
		public MenuData<Loadout> getMenuData() {
			return new MenuData<Loadout>(selectionModel.getSelected());
		}

		@Override
		public JMenu getFilterMenu() {
			return null;
		}

		@Override
		public JMenu getColumnMenu() {
			return tableFormat.getMenu(program, tableModel, jTable, NAME);
		}

		@Override
		public void addInfoMenu(JComponent jComponent) {
			JMenuInfo.module(jComponent, selectionModel.getSelected());
		}

		@Override
		public void addToolMenu(JComponent jComponent) { }
	}

	private class ListenerClass implements ActionListener {
		@Override
		public void actionPerformed(final ActionEvent e) {
			if (LoadoutsAction.OWNERS.name().equals(e.getActionCommand())) {
				String owner = (String) jOwners.getSelectedItem();
				List<String> charShips = new ArrayList<String>();
				for (MyAsset asset : program.getAssetList()) {
					String key = asset.getName() + " #" + asset.getItemID();
					if (!asset.getItem().getCategory().equals(SHIP_CATEGORY) || !asset.isSingleton()) {
						continue;
					}
					if (!owner.equals(asset.getOwner()) && !owner.equals(General.get().all())) {
						continue;
					}
					charShips.add(key);
				}
				if (!charShips.isEmpty()) {
					Collections.sort(charShips, new CaseInsensitiveComparator());
					jExpand.setEnabled(true);
					jCollapse.setEnabled(true);
					jExport.setEnabled(true);
					jOwners.setEnabled(true);
					jShips.setEnabled(true);
					String selectedItem = (String) jShips.getSelectedItem();
					jShips.setModel(new ListComboBoxModel<String>(charShips));
					if (selectedItem != null && charShips.contains(selectedItem)) {
						jShips.setSelectedItem(selectedItem);
					} else {
						jShips.setSelectedIndex(0);
					}
				} else {
					jExpand.setEnabled(false);
					jCollapse.setEnabled(false);
					jExport.setEnabled(false);
					jShips.setEnabled(false);
					jShips.setModel(new ListComboBoxModel<String>());
					jShips.getModel().setSelectedItem(TabsLoadout.get().no1());
				}
			}
			if (LoadoutsAction.FILTER.name().equals(e.getActionCommand())) {
				String selectedShip = (String) jShips.getSelectedItem();
				filterList.setMatcher(new Loadout.LoadoutMatcher(selectedShip));
			}
			if (LoadoutsAction.COLLAPSE.name().equals(e.getActionCommand())) {
				jTable.expandSeparators(false);
			}
			if (LoadoutsAction.EXPAND.name().equals(e.getActionCommand())) {
				jTable.expandSeparators(true);
			}
			if (LoadoutsAction.EXPORT_LOADOUT_SELECTED.name().equals(e.getActionCommand())) {
				loadoutsExportDialog.setVisible(true);
			}
			if (LoadoutsAction.EXPORT_LOADOUT_ALL.name().equals(e.getActionCommand())) {
				String filename = browse();
				List<MyAsset> ships = new ArrayList<MyAsset>();
				for (MyAsset asset : program.getAssetList()) {
					if (!asset.getItem().getCategory().equals(SHIP_CATEGORY) || !asset.isSingleton()) {
						continue;
					}
					ships.add(asset);
				}
				if (filename != null) {
					EveFittingWriter.save(new ArrayList<MyAsset>(ships), filename);
				}
			}
			if (LoadoutsAction.EXPORT.name().equals(e.getActionCommand())) {
				exportDialog.setVisible(true);
			}
		}
	}

	private class LoadoutsFilterControl extends ExportFilterControl<Loadout> {
		@Override
		protected EnumTableColumn<?> valueOf(final String column) {
			try {
				return LoadoutTableFormat.valueOf(column);
			} catch (IllegalArgumentException exception) {

			}
			try {
				return LoadoutExtendedTableFormat.valueOf(column);
			} catch (IllegalArgumentException exception) {

			}
			throw new RuntimeException("Fail to parse filter column: " + column);
		}

		@Override
		protected List<EnumTableColumn<Loadout>> getShownColumns() {
			return new ArrayList<EnumTableColumn<Loadout>>(tableFormat.getShownColumns());
		}

		@Override
		protected void saveSettings(final String msg) {
			program.saveSettings("Ship Loudouts Table: " + msg); //Save Ship Loudout Export Setttings (Filters not used)
		}
	}
}
