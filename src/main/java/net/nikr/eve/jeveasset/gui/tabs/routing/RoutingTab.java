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
package net.nikr.eve.jeveasset.gui.tabs.routing;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.nikr.eve.jeveasset.Program;
import net.nikr.eve.jeveasset.SplashUpdater;
import net.nikr.eve.jeveasset.data.Jump;
import net.nikr.eve.jeveasset.data.MyLocation;
import net.nikr.eve.jeveasset.data.Settings;
import net.nikr.eve.jeveasset.data.SolarSystem;
import net.nikr.eve.jeveasset.data.StaticData;
import net.nikr.eve.jeveasset.gui.images.Images;
import net.nikr.eve.jeveasset.gui.shared.Formater;
import net.nikr.eve.jeveasset.gui.shared.components.JDropDownButton;
import net.nikr.eve.jeveasset.gui.shared.components.JFixedToolBar;
import net.nikr.eve.jeveasset.gui.shared.components.JMainTab;
import net.nikr.eve.jeveasset.gui.shared.components.ListComboBoxModel;
import net.nikr.eve.jeveasset.gui.tabs.assets.MyAsset;
import net.nikr.eve.jeveasset.gui.tabs.overview.OverviewGroup;
import net.nikr.eve.jeveasset.gui.tabs.overview.OverviewLocation;
import net.nikr.eve.jeveasset.gui.tabs.overview.OverviewLocation.LocationType;
import net.nikr.eve.jeveasset.i18n.General;
import net.nikr.eve.jeveasset.i18n.TabsRouting;
import net.nikr.eve.jeveasset.io.shared.ApiIdConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.me.candle.eve.graph.DisconnectedGraphException;
import uk.me.candle.eve.graph.Edge;
import uk.me.candle.eve.graph.Graph;
import uk.me.candle.eve.graph.Node;
import uk.me.candle.eve.graph.distances.Jumps;
import uk.me.candle.eve.routing.Progress;
import uk.me.candle.eve.routing.RoutingAlgorithm;
import uk.me.candle.eve.routing.cancel.CancelService;

/**
 *
 * @author Candle
 */
public class RoutingTab extends JMainTab  {

	private static final  Logger LOG = LoggerFactory.getLogger(RoutingTab.class);

	private enum RoutingAction {
		ADD,
		REMOVE,
		ADD_SYSTEM,
		SOURCE,
		ALGORITHM,
		ALGORITHM_HELP,
		CALCULATE,
		AVOID_ADD,
		AVOID_REMOVE,
		AVOID_CLEAR,
		AVOID_SAVE,
		AVOID_LOAD,
		AVOID_MANAGE,
		SAVE
	}
	//Routing
	private JLabel jAlgorithmLabel;
	private JComboBox<RoutingAlgorithmContainer> jAlgorithm;
	private JButton jAlgorithmInfo;
	private JLabel jFilterLabel;
	private JLabel jFilterSecurityIcon;
	private JLabel jFilterSecurity;
	private JLabel jFilterSystemIcon;
	private JLabel jFilterSystem;
	private JLabel jSourceLabel;
	private JComboBox<SourceItem> jSource;
	private JLabel jStartLabel;
	private JTextField jStart;
	private MoveJList<SolarSystem> jAvailable;
	private JLabel jAvailableRemaining;
	private JButton jAdd;
	private JButton jRemove;
	private JButton jAddSystem;
	private MoveJList<SolarSystem> jWaypoints;
	private JLabel jWaypointsRemaining;
	//Filter
	private JList<SolarSystem> jAvoid;
	private EditableListModel<SolarSystem> avoidModel = new EditableListModel<SolarSystem>();
	private JButton jAvoidAdd;
	private JButton jAvoidRemove;
	private JButton jAvoidClear;
	private JButton jAvoidSave;
	private JDropDownButton jAvoidLoad;
	private JLabel jSecurityIcon;
	private JComboBox<Double> jSecurityMinimum;
	private JLabel jSecuritySeparatorLabel;
	private JComboBox<Double> jSecurityMaximum;
	//Progress
	private JProgressBar jProgress;
	private JButton jCalculate;
	//Result
	private JTextArea jResult;
	private JTextArea jFullResult;
	private JTextArea jInfo;
	//Dialogs
	private JSystemDialog jSystemDialog;
	private JSaveSystemList jSaveSystemList;
	private JManageSystemList jManageSystemList;

	private ListenerClass listener;
	private RouteFind routeFind;

	//Data
	protected Graph filteredGraph;
	private double lastSecMin = 0.0;
	private double lastSecMax = 1.0;
	private List<Long> lastAvoid = new ArrayList<Long>();
	private boolean uiEnabled = true;
	/**
	 *
	 * @param load does nothing except change the signature.
	 */
	protected RoutingTab(final boolean load) {
		super(load);
	}

	public RoutingTab(final Program program) {
		super(program, TabsRouting.get().routingTitle(), Images.TOOL_ROUTING.getIcon(), true);

		listener = new ListenerClass();

		jSystemDialog = new JSystemDialog(program);
		jSaveSystemList = new JSaveSystemList(program);
		jManageSystemList = new JManageSystemList(this, program);

	//Routing
		JPanel jRoutingPanel = new JPanel();
		GroupLayout routingLayout = new GroupLayout(jRoutingPanel);
		jRoutingPanel.setLayout(routingLayout);
		routingLayout.setAutoCreateGaps(true);
		routingLayout.setAutoCreateContainerGaps(true);

		jAlgorithmLabel = new JLabel(TabsRouting.get().algorithm());

		jAlgorithm = new JComboBox<RoutingAlgorithmContainer>(new ListComboBoxModel<RoutingAlgorithmContainer>(RoutingAlgorithmContainer.getRegisteredList()));
		jAlgorithm.setSelectedIndex(0);
		jAlgorithm.setActionCommand(RoutingAction.ALGORITHM.name());
		jAlgorithm.addActionListener(listener);

		jAlgorithmInfo = new JButton(Images.MISC_HELP.getIcon());
		jAlgorithmInfo.setActionCommand(RoutingAction.ALGORITHM_HELP.name());
		jAlgorithmInfo.addActionListener(listener);

		jFilterLabel = new JLabel(TabsRouting.get().filters());
		jFilterSecurityIcon = new JLabel();
		jFilterSecurity = new JLabel();
		jFilterSystemIcon = new JLabel(Images.LOC_SYSTEM.getIcon());
		jFilterSystem = new JLabel();
		jFilterSystem.setIconTextGap(4);

		jSourceLabel = new JLabel(TabsRouting.get().source());

		jSource = new JComboBox<SourceItem>();
		jSource.setActionCommand(RoutingAction.SOURCE.name());
		jSource.addActionListener(listener);

		//Start system
		jStartLabel = new JLabel(TabsRouting.get().startSystem());

		jStart = new JTextField();
		jStart.setEditable(false);
		jStart.setFocusable(false);

		Comparator<SolarSystem> comp = new Comparator<SolarSystem>() {
			@Override
			public int compare(final SolarSystem o1, final SolarSystem o2) {
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		};

		jAvailable = new MoveJList<SolarSystem>(new EditableListModel<SolarSystem>());
		jAvailable.getEditableModel().setSortComparator(comp);
		jAvailable.addMouseListener(listener);
		jAvailable.addListSelectionListener(listener);

		jAvailableRemaining = new JLabel();

		jAdd = new JButton(TabsRouting.get().add());
		jAdd.setActionCommand(RoutingAction.ADD.name());
		jAdd.addActionListener(listener);

		jRemove = new JButton(TabsRouting.get().remove());
		jRemove.setActionCommand(RoutingAction.REMOVE.name());
		jRemove.addActionListener(listener);

		jAddSystem = new JButton(TabsRouting.get().addSystem());
		jAddSystem.setActionCommand(RoutingAction.ADD_SYSTEM.name());
		jAddSystem.addActionListener(listener);

		jWaypoints = new MoveJList<SolarSystem>(new EditableListModel<SolarSystem>());
		jWaypoints.getEditableModel().setSortComparator(comp);
		jWaypoints.addMouseListener(listener);
		jWaypoints.addListSelectionListener(listener);

		jWaypointsRemaining = new JLabel();

		JScrollPane jAvailableScroll = new JScrollPane(jAvailable);
		JScrollPane jWaypointsScroll = new JScrollPane(jWaypoints);

		routingLayout.setHorizontalGroup(
			routingLayout.createSequentialGroup()
				.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(routingLayout.createSequentialGroup()
						.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(jAlgorithmLabel)
							.addComponent(jSourceLabel)
						)
						.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addGroup(routingLayout.createSequentialGroup()
								.addComponent(jAlgorithm)
								.addGap(10)
								.addComponent(jAlgorithmInfo)
							)
							.addComponent(jSource)
						)	
					)
					.addComponent(jAvailableScroll, 300, 300, Short.MAX_VALUE)
					.addComponent(jAvailableRemaining, 300, 300, Short.MAX_VALUE)
				)
				.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(jAdd, 80, 80, 80)
					.addComponent(jRemove, 80, 80, 80)
					.addComponent(jAddSystem, 80, 80, 80)
				)
				.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(routingLayout.createSequentialGroup()
						.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(jStartLabel)
							.addComponent(jFilterLabel)
						)
						.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addGroup(routingLayout.createSequentialGroup()
								.addComponent(jFilterSecurityIcon)
								.addGap(0)
								.addComponent(jFilterSecurity)
								.addGap(20)
								.addComponent(jFilterSystemIcon)
								.addGap(4)
								.addComponent(jFilterSystem)
							)
							.addComponent(jStart)
						)
					)
					.addComponent(jWaypointsScroll, 300, 300, Integer.MAX_VALUE)
					.addComponent(jWaypointsRemaining, 300, 300, Integer.MAX_VALUE)
				)
		);
		routingLayout.setVerticalGroup(
			routingLayout.createSequentialGroup()
				.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
					.addComponent(jAlgorithmLabel, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addComponent(jAlgorithm, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addComponent(jAlgorithmInfo, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addComponent(jFilterLabel, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addComponent(jFilterSecurityIcon, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addComponent(jFilterSecurity, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addComponent(jFilterSystemIcon, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addComponent(jFilterSystem, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
				)
				.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
					.addComponent(jSourceLabel, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addComponent(jSource, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addComponent(jStartLabel, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addComponent(jStart, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
				)
				.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.CENTER, false)
					.addComponent(jAvailableScroll, 130, 130, Integer.MAX_VALUE)
					.addComponent(jWaypointsScroll, 130, 130, Integer.MAX_VALUE)
					.addGroup(routingLayout.createSequentialGroup()
						.addComponent(jAdd, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
						.addComponent(jRemove, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
						.addComponent(jAddSystem, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					)
				)
				.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
					.addComponent(jAvailableRemaining, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
					.addComponent(jWaypointsRemaining, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
				)
		);
	//Filters
		JPanel jFilterPanel = new JPanel();
		GroupLayout filterLayout = new GroupLayout(jFilterPanel);
		jFilterPanel.setLayout(filterLayout);
		filterLayout.setAutoCreateGaps(true);
		filterLayout.setAutoCreateContainerGaps(true);

		JPanel jAvoidPanel = new JPanel();
		jAvoidPanel.setBorder(BorderFactory.createTitledBorder(TabsRouting.get().avoid()));
		GroupLayout avoidLayout = new GroupLayout(jAvoidPanel);
		jAvoidPanel.setLayout(avoidLayout);
		avoidLayout.setAutoCreateGaps(true);
		avoidLayout.setAutoCreateContainerGaps(true);

		JPanel jSecurityPanel = new JPanel();
		jSecurityPanel.setBorder(BorderFactory.createTitledBorder(TabsRouting.get().security()));
		GroupLayout securityLayout = new GroupLayout(jSecurityPanel);
		jSecurityPanel.setLayout(securityLayout);
		securityLayout.setAutoCreateGaps(true);
		securityLayout.setAutoCreateContainerGaps(true);

		avoidModel.setSortComparator(comp);
		avoidModel.addAll(Settings.get().getRoutingSettings().getAvoid().values());

		jAvoid = new JList<SolarSystem>(avoidModel);
		jAvoid.addMouseListener(listener);
		jAvoid.addListSelectionListener(listener);

		JFixedToolBar jToolBar = new JFixedToolBar(JFixedToolBar.Orientation.VERTICAL);

		jAvoidAdd = new JButton(TabsRouting.get().avoidAdd(), Images.EDIT_ADD.getIcon());
		jAvoidAdd.setActionCommand(RoutingAction.AVOID_ADD.name());
		jAvoidAdd.addActionListener(listener);
		jToolBar.addButton(jAvoidAdd);

		jAvoidRemove = new JButton(TabsRouting.get().avoidRemove(), Images.EDIT_DELETE.getIcon());
		jAvoidRemove.setActionCommand(RoutingAction.AVOID_REMOVE.name());
		jAvoidRemove.addActionListener(listener);
		jAvoidRemove.setEnabled(false);
		jToolBar.addButton(jAvoidRemove);

		jAvoidClear = new JButton(TabsRouting.get().avoidClear(), Images.FILTER_CLEAR.getIcon());
		jAvoidClear.setActionCommand(RoutingAction.AVOID_CLEAR.name());
		jAvoidClear.addActionListener(listener);
		jToolBar.addButton(jAvoidClear);

		jAvoidSave = new JButton(TabsRouting.get().avoidSave(), Images.FILTER_SAVE.getIcon());
		jAvoidSave.setActionCommand(RoutingAction.AVOID_SAVE.name());
		jAvoidSave.addActionListener(listener);
		jToolBar.addButton(jAvoidSave);
	
		jAvoidLoad = new JDropDownButton(TabsRouting.get().avoidLoad(), Images.FILTER_LOAD.getIcon());
		jToolBar.addButton(jAvoidLoad);

		JScrollPane jAvoidScroll = new JScrollPane(jAvoid);

		Double[] security = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};

		jSecurityIcon = new JLabel();

		jSecurityMinimum = new JComboBox<Double>(security);
		jSecurityMinimum.setSelectedItem(Settings.get().getRoutingSettings().getSecMin());
		jSecurityMinimum.setActionCommand(RoutingAction.SAVE.name());
		jSecurityMinimum.addActionListener(listener);

		jSecuritySeparatorLabel = new JLabel(" - ");

		jSecurityMaximum = new JComboBox<Double>(security);
		jSecurityMaximum.setSelectedItem(Settings.get().getRoutingSettings().getSecMax());
		jSecurityMaximum.setActionCommand(RoutingAction.SAVE.name());
		jSecurityMaximum.addActionListener(listener);

		updateFilterLabels();
		updateSavedFilters();

		avoidLayout.setHorizontalGroup(
			avoidLayout.createSequentialGroup()
				.addComponent(jAvoidScroll, 300, 300, Integer.MAX_VALUE)
				.addComponent(jToolBar)
		);
		avoidLayout.setVerticalGroup(
			avoidLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
				.addComponent(jAvoidScroll, 160, 160, Integer.MAX_VALUE)
				.addComponent(jToolBar)
		);
		securityLayout.setHorizontalGroup(
			securityLayout.createSequentialGroup()
				.addComponent(jSecurityIcon)
				.addComponent(jSecurityMinimum, 80, 80, Short.MAX_VALUE)
				.addComponent(jSecuritySeparatorLabel)
				.addComponent(jSecurityMaximum, 80, 80, Short.MAX_VALUE)
		);
		securityLayout.setVerticalGroup(
			securityLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
				.addComponent(jSecurityIcon, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
				.addComponent(jSecurityMinimum, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
				.addComponent(jSecuritySeparatorLabel, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())	
				.addComponent(jSecurityMaximum, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
		);
		filterLayout.setHorizontalGroup(
			filterLayout.createSequentialGroup()
				.addComponent(jAvoidPanel)
				.addComponent(jSecurityPanel)
		);
		filterLayout.setVerticalGroup(
			filterLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
				.addComponent(jAvoidPanel)
				.addComponent(jSecurityPanel)
		);
	//Progress
		jProgress = new JProgressBar();
		jProgress.setValue(0);
		jProgress.setMaximum(100);
		jProgress.setMinimum(0);

		jCalculate = new JButton(TabsRouting.get().calculate());
		jCalculate.setActionCommand(RoutingAction.CALCULATE.name());
		jCalculate.addActionListener(listener);
	//Result
		jResult = new JTextArea();
		jResult.setEditable(false);
		jResult.setFont(jPanel.getFont());

		jFullResult = new JTextArea();
		jFullResult.setEditable(false);
		jFullResult.setFont(jPanel.getFont());

		jInfo = new JTextArea();
		jInfo.setEditable(false);
		jInfo.setFont(jPanel.getFont());

		final JScrollPane jResultScroll = new JScrollPane(jResult, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		JScrollPane jFullResultScroll = new JScrollPane(jFullResult, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		jFullResultScroll.getVerticalScrollBar().setModel(jResultScroll.getVerticalScrollBar().getModel());
		jFullResultScroll.setWheelScrollingEnabled(false);
		jFullResultScroll.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				jResultScroll.dispatchEvent(e);
			}
		});
		JScrollPane jInfoScroll = new JScrollPane(jInfo);

		JTabbedPane jResultTabs = new JTabbedPane();
		jResultTabs.addTab(TabsRouting.get().resultTabShort(), jResultScroll);
		jResultTabs.addTab(TabsRouting.get().resultTabFull(), jFullResultScroll);
		jResultTabs.addTab(TabsRouting.get().resultTabInfo(), jInfoScroll);

		JTabbedPane jSystemTabs = new JTabbedPane();
		jSystemTabs.addTab(TabsRouting.get().routingTab(), jRoutingPanel);
		jSystemTabs.addTab(TabsRouting.get().filtersTab(), jFilterPanel);

		// widths are defined in here.
		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(jSystemTabs)
				.addGroup(layout.createSequentialGroup()
					.addComponent(jProgress, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
					.addComponent(jCalculate)
				)
				.addComponent(jResultTabs)
			);
		// heights are defined here.
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				.addComponent(jSystemTabs, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
					.addComponent(jProgress, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())	
					.addComponent(jCalculate, Program.getButtonsHeight(), Program.getButtonsHeight(), Program.getButtonsHeight())
				)
				.addComponent(jResultTabs)
				
		);
		buildGraph(true); //Build default Graph (0.0/All sec - no avoids)
		jSystemDialog.updateData(filteredGraph.getNodes()); //Will be replaced by valid systems by processRouteInner()
	}

	@Override
	public void clearData() {
		RoutingAlgorithm.setCache(false); //Clear cache
	}

	@Override
	public void updateData() {
		//Do everything the constructor does...
		jAvailable.getEditableModel().clear();
		jWaypoints.getEditableModel().clear();
		List<SourceItem> sources = new ArrayList<SourceItem>();
		for (Entry<String, OverviewGroup> entry : Settings.get().getOverviewGroups().entrySet()) {
			sources.add(new SourceItem(entry.getKey(), true));
		}
		Collections.sort(sources);
		sources.add(0, new SourceItem(TabsRouting.get().filteredAssets()));
		sources.add(0, new SourceItem(General.get().all()));
		jSource.setModel(new ListComboBoxModel<SourceItem>(sources));
		jAlgorithm.setSelectedIndex(0);
		jResult.setText(TabsRouting.get().emptyResult());
		jResult.setCaretPosition(0);
		jResult.setEnabled(false);
		jFullResult.setText(TabsRouting.get().emptyResult());
		jFullResult.setCaretPosition(0);
		jFullResult.setEnabled(false);
		jInfo.setText(TabsRouting.get().emptyResult());
		jInfo.setCaretPosition(0);
		jInfo.setEnabled(false);
		
		updateRemaining();
		processFilteredAssets();
	}

	public void addSystems(int max) {
		Set<SolarSystem> allLocs = new HashSet<SolarSystem>();
		allLocs.addAll(jAvailable.getEditableModel().getAll());
		allLocs.addAll(jWaypoints.getEditableModel().getAll());
		int count = 0;
		for (MyLocation location : StaticData.get().getLocations().values()) {
			if (count >= max) {
				break;
			}
			if (location.getSecurityObject().getDouble() < (Double)jSecurityMinimum.getSelectedItem()) {
				continue;
			}
			SolarSystem loc = findNodeForLocation(filteredGraph, location.getSystemID());
			if (loc != null) {
				boolean add = allLocs.add(loc);
				if (add) {
					count++;
				}
			} else {
				LOG.debug("ignoring {}", location);
			}
		}
		jAvailable.getEditableModel().clear();
		jWaypoints.getEditableModel().clear();
		jAvailable.getEditableModel().addAll(allLocs);
		updateRemaining();
	}

	public SolarSystem getSolarSystem() {
		return jSystemDialog.show();
	}

	private void updateRemaining() {
		updateWaypointsRemaining();
		updateAvailableRemaining();
		validateLists();
	}

	private void updateWaypointsRemaining() {
		int max = ((RoutingAlgorithmContainer) jAlgorithm.getSelectedItem()).getWaypointLimit();
		int cur = jWaypoints.getModel().getSize();
		if (max < cur) {
			jWaypointsRemaining.setForeground(Color.RED);
		} else if (max == cur) {
			jWaypointsRemaining.setForeground(Color.BLUE);
		} else {
			jWaypointsRemaining.setForeground(Color.BLACK);
		}
		jWaypointsRemaining.setText(TabsRouting.get().allowed(cur, max));
	}

	private void updateAvailableRemaining() {
		int cur = jAvailable.getModel().getSize();
		int tot = cur + jWaypoints.getModel().getSize();
		jAvailableRemaining.setText(TabsRouting.get().total(cur, tot));
	}

	protected final void buildGraph(boolean all) {
		// build the graph.
		// filter the solarsystems based on the settings.
		if (filteredGraph != null) {
			filteredGraph.clear();
		}
		filteredGraph = new Graph(new Jumps());
		double secMin;
		double secMax;
		if (jSecurityMinimum != null) {
			secMin = (Double) jSecurityMinimum.getSelectedItem();
		} else {
			secMin = 0.0f;
		}
		if (jSecurityMaximum != null) {
			secMax = (Double) jSecurityMaximum.getSelectedItem();
		} else {
			secMax = 1.0f;
		}
		int count = 0;
		Map<Long, SolarSystem> systemCache = new HashMap<Long, SolarSystem>();
		for (Jump jump : StaticData.get().getJumps()) { // this way we exclude the locations that are unreachable.
			count++;
			SplashUpdater.setSubProgress((int) (count * 100.0 / StaticData.get().getJumps().size()));

			SolarSystem from = systemCache.get(jump.getFrom().getSystemID());
			SolarSystem to = systemCache.get(jump.getTo().getSystemID());
			if (from == null) {
				from = new SolarSystem(jump.getFrom());
				systemCache.put(from.getSystemID(), from);
			}
			if (to == null) {
				to = new SolarSystem(jump.getTo());
				systemCache.put(to.getSystemID(), to);
			}
			if (all || (jump.getFrom().getSecurityObject().getDouble() >= secMin
						&& jump.getTo().getSecurityObject().getDouble() >= secMin
						&& jump.getFrom().getSecurityObject().getDouble() <= secMax
						&& jump.getTo().getSecurityObject().getDouble() <= secMax
						&& !Settings.get().getRoutingSettings().getAvoid().keySet().contains(jump.getFrom().getSystemID())
						&& !Settings.get().getRoutingSettings().getAvoid().keySet().contains(jump.getTo().getSystemID())
					)) {
				filteredGraph.addEdge(new Edge(from, to));
			}
		}
		SplashUpdater.setSubProgress(100);
		systemCache.clear();
	}

	protected void processFilteredAssets() {
		// select the active places.
		SortedSet<SolarSystem> allLocs = new TreeSet<SolarSystem>(new Comparator<SolarSystem>() {
			@Override
			public int compare(final SolarSystem o1, final SolarSystem o2) {
				String n1 = o1.getName();
				String n2 = o2.getName();
				return n1.compareToIgnoreCase(n2);
			}
		});
		jAvailable.getEditableModel().addAll(allLocs);
		List<MyAsset> assets;
		SourceItem source = (SourceItem) jSource.getSelectedItem();
		if (source.getName().equals(General.get().all())) { //ALL
			assets = new ArrayList<MyAsset>(program.getAssetList());
		} else if (source.getName().equals(TabsRouting.get().filteredAssets())) { //FILTERS
			assets = program.getAssetsTab().getFilteredAssets();
		} else { //OVERVIEW GROUP
			assets = new ArrayList<MyAsset>();
			OverviewGroup group = Settings.get().getOverviewGroups().get(source.getName());
			for (OverviewLocation location : group.getLocations()) {
				for (MyAsset asset : program.getAssetList()) {
					if ((location.getName().equals(asset.getLocation().getLocation()))
						|| (location.getType() == LocationType.TYPE_SYSTEM && location.getName().equals(asset.getLocation().getSystem()))
						|| (location.getType() == LocationType.TYPE_REGION && location.getName().equals(asset.getLocation().getRegion()))
						) {
						assets.add(asset);
					}
				}
			}
		}
		for (MyAsset ea : assets) {
			SolarSystem loc = findNodeForLocation(filteredGraph, ea.getLocation().getSystemID());
			if (loc != null) {
				allLocs.add(loc);
			} else {
				LOG.debug("ignoring {}", ea.getLocation().getLocation());
			}
		}

		jAvailable.getEditableModel().addAll(allLocs);
		updateRemaining();
	}

	/**
	 *
	 * @param g
	 * @param locationID
	 * @return null if the system is unreachable (e.g. w-space)
	 */
	private SolarSystem findNodeForLocation(final Graph g, final long locationID) {
		if (locationID < 0) {
			throw new RuntimeException("Unknown Location:" + locationID);
		}
		for (Node n : g.getNodes()) {
			if (n instanceof SolarSystem) {
				SolarSystem ss = (SolarSystem) n;
				if (ss.getSystemID() == locationID) {
					return ss;
				}
			}
		}
		MyLocation location = ApiIdConverter.getLocation(locationID);
		if (location != null) {
			location = ApiIdConverter.getLocation(location.getSystemID());
		}
		if (location != null) {
			return new SolarSystem(location);
		} else {
			return null;
		}
	}

	/**
	 * Moves the selectewd items in the 'from' JList to the 'to' JList.
	 *
	 * @param from
	 * @param to
	 * @param limit
	 * @return true if all the items were moved.
	 */
	private boolean move(final MoveJList<SolarSystem> from, final MoveJList<SolarSystem> to, final int limit) {
		boolean b = from.move(to, limit);
		to.requestFocusInWindow();
		updateRemaining();
		return b;
	}

	private void processRoute() {
		//Disable the UI controls
		setUIEnabled(false);
		//Reset Progress
		jProgress.setValue(0);
		jProgress.setIndeterminate(true);
		//Create Thread
		routeFind = new RouteFind();
		//Add progress listener
		routeFind.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("progress".equals(evt.getPropertyName())) {
		            int progress = (Integer) evt.getNewValue();
					if (jProgress.isIndeterminate() && progress > 0) {
						jProgress.setIndeterminate(false);
					}
					jProgress.setValue(progress);
				}
			}
		});
		//Start Thread
		routeFind.execute();
	}

	private void processRouteInner() {
		try {
			//Update Graph if needed (AKA filter has changed)
			if (lastSecMin != (Double) jSecurityMinimum.getSelectedItem()
					|| lastSecMax != (Double) jSecurityMaximum.getSelectedItem()
					|| !lastAvoid.equals(new ArrayList<Long>(Settings.get().getRoutingSettings().getAvoid().keySet()))) {
				buildGraph(false);
				lastSecMin = (Double) jSecurityMinimum.getSelectedItem();
				lastSecMax = (Double) jSecurityMaximum.getSelectedItem();
				lastAvoid = new ArrayList<Long>(Settings.get().getRoutingSettings().getAvoid().keySet());
			}
			//Warning for 2 or less systems
			if (jWaypoints.getModel().getSize() <= 2) {
				JOptionPane.showMessageDialog(program.getMainWindow().getFrame(), TabsRouting.get().noSystems(), TabsRouting.get().noSystemsTitle(), JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			//Clear previous results
			jResult.setText(TabsRouting.get().emptyResult());
			jResult.setCaretPosition(0);
			jResult.setEnabled(false);
			jFullResult.setText(TabsRouting.get().emptyResult());
			jFullResult.setCaretPosition(0);
			jFullResult.setEnabled(false);
			jResult.setCaretPosition(0);
			jInfo.setText(TabsRouting.get().emptyResult());
			jInfo.setCaretPosition(0);
			jInfo.setEnabled(false);
			//Update all SolarSystem with the latest from the new Graph
			//This is needed to get the proper Edge(s) parsed to the routing Algorithm
			List<Node> inputWaypoints = new ArrayList<Node>();
			for (SolarSystem solarSystem : jWaypoints.getEditableModel().getAll()) {
				inputWaypoints.add(findNodeForLocation(filteredGraph, solarSystem.getSystemID()));
			}
			//Move frist system to the top....
			final String text = jStart.getText();
			if (!text.contains(TabsRouting.get().startEmpty())) {
				Collections.sort(inputWaypoints, new Comparator<Node>() {
					@Override
					public int compare(Node o1, Node o2) {
						if (o1.getName().equals(text) && o2.getName().equals(text)) {
							return 0; //Equal
						} else if (o1.getName().equals(text)) {
							return -1; //Before
						} else if (o2.getName().equals(text)) {
							return 1; //After
						} else {
							return o1.getName().compareTo(o2.getName());
						}
					}
				});
			}
			//Start route finding:
			RoutingAlgorithmContainer algorithm = (RoutingAlgorithmContainer) jAlgorithm.getSelectedItem();
			List<Node> route = executeRouteFinding(inputWaypoints, algorithm);
			if (route.isEmpty()) { //Cancelled
				algorithm.resetCancelService();
				return;
			} else { //Completed!
				jProgress.setValue(jProgress.getMaximum());
			}
		//Info Result
			final StringBuilder infoString = new StringBuilder();
			//algorithm name
			String name = algorithm.getName();
			//generation time
			String time = Formater.milliseconds(algorithm.getLastTimeTaken());
			//jumps
			int jumps = algorithm.getLastDistance();
			//avoding systems
			final StringBuilder avoidingString = new StringBuilder();
			for (SolarSystem avoidSystem : Settings.get().getRoutingSettings().getAvoid().values()) {
				if (!avoidingString.toString().isEmpty()) {
					avoidingString.append(", ");
				}
				avoidingString.append(avoidSystem.getName());
			}
			if (avoidingString.toString().isEmpty()) {
				avoidingString.append(TabsRouting.get().avoidNone());
			}
			//security
			final StringBuilder securityString = new StringBuilder();
			securityString.append(Formater.securityFormat(jSecurityMinimum.getSelectedItem()));
			securityString.append(" - ");
			securityString.append(Formater.securityFormat(jSecurityMaximum.getSelectedItem()));
			//Done
			infoString.append(TabsRouting.get().resultText(name,
					jumps,
					inputWaypoints.size(),
					securityString.toString(),
					avoidingString.toString(),
					time));
		//Route Result
			Node lastNode = null;
			final StringBuilder fullRouteString = new StringBuilder();
			final StringBuilder routeString = new StringBuilder();
			for (Node node : route) {
				if (lastNode != null) {
					boolean first = true;
					for (Node subNode : filteredGraph.routeBetween(lastNode, node)) {
						if (first) {
							first = false;
							fullRouteString.append(subNode.getName());
						} else {
							fullRouteString.append(TabsRouting.get().resultArrow());
							fullRouteString.append(subNode.getName());
						}
					}
					fullRouteString.append('\n');
					routeString.append('\n');
				}
				routeString.append(node.getName());
				lastNode = node;
			}
			//last node to first node
			boolean first = true;
			for (Node subNode : filteredGraph.routeBetween(lastNode, route.get(0))) {
				if (first) {
					first = false;
					fullRouteString.append(subNode.getName());
				} else {
					fullRouteString.append(TabsRouting.get().resultArrow());
					fullRouteString.append(subNode.getName());
				}
			}
			//Set results
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					jResult.setText(routeString.toString());
					jResult.setEnabled(true);
					jResult.setCaretPosition(0);
					jFullResult.setText(fullRouteString.toString());
					jFullResult.setEnabled(true);
					jFullResult.setCaretPosition(0);
					jInfo.setText(infoString.toString());
					jInfo.setEnabled(true);
				}
			});
		} catch (DisconnectedGraphException dce) {
			JOptionPane.showMessageDialog(program.getMainWindow().getFrame()
							, dce.getMessage()
							, TabsRouting.get().error()
							, JOptionPane.ERROR_MESSAGE);
		}
	}

	protected Graph getGraph() {
		return filteredGraph;
	}

	private List<Node> executeRouteFinding(final List<Node> inputWaypoints, final RoutingAlgorithmContainer algorithm) {
		return algorithm.execute(routeFind, filteredGraph, inputWaypoints);
	}

	private void setUIEnabled(final boolean b) {
		uiEnabled = b;
		//Routing
		jAlgorithmLabel.setEnabled(b);
		jAlgorithm.setEnabled(b);
		jAlgorithmInfo.setEnabled(b);
		jFilterLabel.setEnabled(b);
		jFilterSecurity.setEnabled(b);
		jFilterSystem.setEnabled(b);
		jSourceLabel.setEnabled(b);
		jSource.setEnabled(b);
		jStartLabel.setEnabled(b);
		if (jStart.getText().contains(TabsRouting.get().startEmpty())) {
			jStart.setEnabled(false);
		} else {
			jStart.setEnabled(b);
		}
		jAvailable.setEnabled(b);
		jAvailableRemaining.setEnabled(b);
		jAdd.setEnabled(b);
		jRemove.setEnabled(b);
		jAddSystem.setEnabled(b);
		jWaypoints.setEnabled(b);
		jWaypointsRemaining.setEnabled(b);
		//Filters
		jAvoid.setEnabled(b);
		jAvoidAdd.setEnabled(b);
		if (b) {
			jAvoidRemove.setEnabled(jAvoid.getSelectedIndices().length > 0);
			jAvoidClear.setEnabled(!avoidModel.getAll().isEmpty());
			jAvoidSave.setEnabled(!avoidModel.getAll().isEmpty());
			jAvoidLoad.setEnabled(!Settings.get().getRoutingSettings().getPresets().isEmpty());
		} else {
			jAvoidRemove.setEnabled(b);
			jAvoidClear.setEnabled(b);
			jAvoidSave.setEnabled(b);
			jAvoidLoad.setEnabled(b);
		}
		jSecurityMinimum.setEnabled(b);
		jSecuritySeparatorLabel.setEnabled(b);
		jSecurityMaximum.setEnabled(b);
		//Process
		if (b) {
			jCalculate.setText(TabsRouting.get().calculate());
		} else {
			jCalculate.setText(TabsRouting.get().cancel());
		}
	}

	private void cancelProcessing() {
		((RoutingAlgorithmContainer) jAlgorithm.getSelectedItem()).getCancelService().cancel();
	}

	private void validateLists() {
		if (uiEnabled) {
			jRemove.setEnabled(jWaypoints.getSelectedIndices().length > 0);
			jAdd.setEnabled(jAvailable.getSelectedIndices().length > 0 && jWaypoints.getModel().getSize() < ((RoutingAlgorithmContainer) jAlgorithm.getSelectedItem()).getWaypointLimit());
			jAddSystem.setEnabled(jWaypoints.getModel().getSize() < ((RoutingAlgorithmContainer) jAlgorithm.getSelectedItem()).getWaypointLimit());
		}
		if (jWaypoints.getSelectedIndices().length == 1) { //Selected OK
			jStart.setText(jWaypoints.getSelectedValue().toString());
			jStart.setEnabled(uiEnabled);
		} else { //Empty List
			List<? extends SolarSystem> all = jWaypoints.getEditableModel().getAll();
			if (!all.isEmpty()) {
				jStart.setText(TabsRouting.get().startEmptyAuto(all.get(0).getName()));
			} else {
				jStart.setText(TabsRouting.get().startEmpty());
			}
			jStart.setEnabled(false);
		}
	}

	private void updateFilterLabels() {
		double secMin = Settings.get().getRoutingSettings().getSecMin();
		double secMax = Settings.get().getRoutingSettings().getSecMax();
		int size = Settings.get().getRoutingSettings().getAvoid().size();
		jFilterSecurity.setText(Formater.securityFormat(secMin) + " - " + Formater.securityFormat(secMax));
		if (secMin == 0.0) {
			jSecurityIcon.setIcon(Images.UPDATE_CANCELLED.getIcon());
			jFilterSecurityIcon.setIcon(Images.UPDATE_CANCELLED.getIcon());
		} else if (secMin >= 0.5) {
			jSecurityIcon.setIcon(Images.UPDATE_DONE_OK.getIcon());
			jFilterSecurityIcon.setIcon(Images.UPDATE_DONE_OK.getIcon());
		} else {
			jSecurityIcon.setIcon(Images.UPDATE_DONE_SOME.getIcon());
			jFilterSecurityIcon.setIcon(Images.UPDATE_DONE_SOME.getIcon());
		}
		jFilterSystem.setText(String.valueOf(size));
		jAvoidClear.setEnabled(!avoidModel.getAll().isEmpty());
		jAvoidSave.setEnabled(!avoidModel.getAll().isEmpty());
		jAvoidLoad.setEnabled(!Settings.get().getRoutingSettings().getPresets().isEmpty());
	}

	private void updateSavedFilters() {
		jAvoidLoad.removeAll();

		JMenuItem jManage = new JMenuItem(TabsRouting.get().avoidManage(), Images.DIALOG_SETTINGS.getIcon());
		jManage.setActionCommand(RoutingAction.AVOID_MANAGE.name());
		jManage.addActionListener(listener);
		jAvoidLoad.add(jManage);

		if (!Settings.get().getRoutingSettings().getPresets().isEmpty()) {
			jAvoidLoad.addSeparator();
		}

		ArrayList<String> presets = new ArrayList<String>(Settings.get().getRoutingSettings().getPresets().keySet());
		Collections.sort(presets);
		for (String name : presets) {
			JMenuItem jMenuItem = new JLoadMenuItem(name, Settings.get().getRoutingSettings().getPresets().get(name));
			jMenuItem.setActionCommand(RoutingAction.AVOID_LOAD.name());
			jMenuItem.addActionListener(listener);
			jAvoidLoad.add(jMenuItem);
		}
		jAvoidLoad.setEnabled(!Settings.get().getRoutingSettings().getPresets().isEmpty());
		jManageSystemList.updateData();
	}

	public void loadFilter(Set<Long> systemIds) {
		avoidModel.clear();
		Settings.lock("Routing (Load Filter)");
		Settings.get().getRoutingSettings().getAvoid().clear();
		for (Long systemID : systemIds) {
			SolarSystem system = new SolarSystem(ApiIdConverter.getLocation(systemID));
			Settings.get().getRoutingSettings().getAvoid().put(system.getSystemID(), system);
			avoidModel.add(system);
		}
		Settings.unlock("Routing (Load Filter)");
		program.saveSettings("Routing (Load Filter)");
		updateFilterLabels();
	}

	public void deleteFilters(List<String> list) {
		Settings.lock("Routing (Delete Filters)");
		for (String filter : list) {
			Settings.get().getRoutingSettings().getPresets().remove(filter);
		}
		Settings.unlock("Routing (Delete Filters)");
		program.saveSettings("Routing (Delete Filters)");
		updateSavedFilters();
	}

	public void renameFilter(String name, String oldName) {
		Settings.lock("Routing (Rename Filter)");
		Set<Long> systemIDs = Settings.get().getRoutingSettings().getPresets().remove(oldName);
		Settings.get().getRoutingSettings().getPresets().put(name, systemIDs);
		Settings.unlock("Routing (Rename Filter)");
		program.saveSettings("Routing (Rename Filter)");
		updateSavedFilters();
	}

	public void mergeFilters(String name, List<String> list) {
		Set<Long> systemIDs = new HashSet<Long>();
		Settings.lock("Routing (Merge Filters)");
		for (String mergeName : list) {
			systemIDs.addAll(Settings.get().getRoutingSettings().getPresets().get(mergeName));
		}
		Settings.get().getRoutingSettings().getPresets().put(name, systemIDs);
		Settings.unlock("Routing (Merge Filters)");
		program.saveSettings("Routing (Merge Filters)");
		updateSavedFilters();
	}

	private void removeSystems() {
		Settings.lock("Routing (Delete Systems)");
		for (SolarSystem system : jAvoid.getSelectedValuesList()) {
			avoidModel.remove(system);
			Settings.get().getRoutingSettings().getAvoid().remove(system.getSystemID());
		}
		Settings.unlock("Routing (Delete Systems)");
		program.saveSettings("Routing (Delete Systems)");
		updateFilterLabels();
	}

	private class ListenerClass extends MouseAdapter implements ActionListener, ListSelectionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {
			LOG.debug(e.getActionCommand());
			if (RoutingAction.ADD.name().equals(e.getActionCommand())) {
				move(jAvailable, jWaypoints, ((RoutingAlgorithmContainer) jAlgorithm.getSelectedItem()).getWaypointLimit());
			} else if (RoutingAction.REMOVE.name().equals(e.getActionCommand())) {
				move(jWaypoints, jAvailable, Integer.MAX_VALUE);
			} else if (RoutingAction.CALCULATE.name().equals(e.getActionCommand())) {
				if (jCalculate.getText().equals(TabsRouting.get().cancel())) {
					cancelProcessing();
				} else {
					if (jResult.isEnabled()) {
						int value = JOptionPane.showConfirmDialog(program.getMainWindow().getFrame(), TabsRouting.get().resultOverwrite(), TabsRouting.get().calculate(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
						if (value != JOptionPane.OK_OPTION) {
							return;
						}
					}
					processRoute();
				}
			} else if (RoutingAction.SOURCE.name().equals(e.getActionCommand())) {
				jAvailable.getEditableModel().clear();
				jWaypoints.getEditableModel().clear();
				processFilteredAssets();
			} else if (RoutingAction.ALGORITHM.name().equals(e.getActionCommand())) {
				updateRemaining();
			} else if (RoutingAction.ALGORITHM_HELP.name().equals(e.getActionCommand())) {
				RoutingAlgorithmContainer rac = ((RoutingAlgorithmContainer) jAlgorithm.getSelectedItem());
				JOptionPane.showMessageDialog(program.getMainWindow().getFrame(), rac.getBasicDescription(), rac.getName(), JOptionPane.INFORMATION_MESSAGE);
			} else if (RoutingAction.AVOID_REMOVE.name().equals(e.getActionCommand())) {
				removeSystems();
			} else if (RoutingAction.AVOID_CLEAR.name().equals(e.getActionCommand())) {
				Settings.lock("Routing (Clear Systems)");
				Settings.get().getRoutingSettings().getAvoid().clear();
				avoidModel.clear();
				updateFilterLabels();
				Settings.unlock("Routing (Clear Systems)");
				program.saveSettings("Routing (Clear Systems)");
			} else if (RoutingAction.AVOID_ADD.name().equals(e.getActionCommand())) {
				SolarSystem system = jSystemDialog.show();
				if (system != null) {
					Settings.lock("Routing (Add System)");
					Settings.get().getRoutingSettings().getAvoid().put(system.getSystemID(), system);
					avoidModel.clear();
					avoidModel.addAll(Settings.get().getRoutingSettings().getAvoid().values());
					Settings.unlock("Routing (Add System)");
					program.saveSettings("Routing (Add System)");
					updateFilterLabels();
				}
			} else if (RoutingAction.AVOID_SAVE.name().equals(e.getActionCommand())) {
				jSaveSystemList.updateData(new ArrayList<String>(Settings.get().getRoutingSettings().getPresets().keySet()));
				String name = jSaveSystemList.show();
				if (name != null) {
					Settings.lock("Routing (Save Filter)");
					Set<Long> systemIDs = new HashSet<Long>();
					for (SolarSystem system : avoidModel.getAll()) {
						systemIDs.add(system.getSystemID());
					}
					Settings.get().getRoutingSettings().getPresets().put(name, systemIDs);
					Settings.unlock("Routing (Save Filter)");
					program.saveSettings("Routing (Save Filter)");
					updateSavedFilters();
				}
			} else if (RoutingAction.AVOID_LOAD.name().equals(e.getActionCommand())) {
				Object source = e.getSource();
				if (source instanceof JLoadMenuItem) {
					JLoadMenuItem menuItem = (JLoadMenuItem) source;
					loadFilter(menuItem.getSystemIDs());
				}
			} else if (RoutingAction.AVOID_MANAGE.name().equals(e.getActionCommand())) {
				jManageSystemList.updateData();
				jManageSystemList.setVisible(true);
			} else if (RoutingAction.SAVE.name().equals(e.getActionCommand())) {
				double min = (Double) jSecurityMinimum.getSelectedItem();
				double max = (Double) jSecurityMaximum.getSelectedItem();
				if (max < min) {
					max = min;
					jSecurityMaximum.setSelectedItem(min);
				}
				Settings.lock("Routing (Security)");
				Settings.get().getRoutingSettings().setSecMin(min);
				Settings.get().getRoutingSettings().setSecMax(max);
				Settings.unlock("Routing (Security)");
				program.saveSettings("Routing (Security)");
				updateFilterLabels();
				
			} else if (RoutingAction.ADD_SYSTEM.name().equals(e.getActionCommand())) {
				SolarSystem system = jSystemDialog.show();
				if (system != null ) {
					if (!jWaypoints.getEditableModel().contains(system)
						&& !jAvailable.getEditableModel().contains(system)) {
						//New
						jWaypoints.getEditableModel().add(system);
					} else if (jAvailable.getEditableModel().contains(system)) {
						//In available: moving to waypoints
						jAvailable.getEditableModel().remove(system);
						jWaypoints.getEditableModel().add(system);
					} //Else: Already in waypoints - do nothing
					updateRemaining();
				}
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1
						&& e.getClickCount()% 2 == 0
						&& !e.isControlDown()
						&& !e.isShiftDown()
						) {
				if (e.getSource().equals(jAvailable) && jAvailable.isEnabled()) {
					move(jAvailable, jWaypoints, ((RoutingAlgorithmContainer) jAlgorithm.getSelectedItem()).getWaypointLimit());
				} else if (e.getSource().equals(jWaypoints) && jWaypoints.isEnabled()) {
					move(jWaypoints, jAvailable, Integer.MAX_VALUE);
				} else if (e.getSource().equals(jAvoid) && jAvoid.isEnabled()) {
					removeSystems();
				}
			}
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (e.getSource().equals(jAvailable) || e.getSource().equals(jWaypoints)) {
				validateLists();
			} else if (e.getSource().equals(jAvoid)) {
				jAvoidRemove.setEnabled(jAvoid.getSelectedIndices().length > 0);
			}
		}
	}

	/**
	 * A GUI compatible container for the routing algorithms.
	 */
	private static class RoutingAlgorithmContainer {

		private RoutingAlgorithm contained;

		public RoutingAlgorithmContainer(final RoutingAlgorithm contained) {
			this.contained = contained;
		}

		public int getWaypointLimit() {
			return contained.getWaypointLimit();
		}

		public String getName() {
			return contained.getName();
		}

		public String getTechnicalDescription() {
			return contained.getTechnicalDescription();
		}

		public String getBasicDescription() {
			return contained.getBasicDescription();
		}

		public List<Node> execute(final Progress progress, final Graph g, final List<? extends Node> assetLocations) {
			return contained.execute(progress, g, assetLocations);
		}

		public long getLastTimeTaken() {
			return contained.getLastTimeTaken();
		}

		public int getLastDistance() {
			return contained.getLastDistance();
		}

		public CancelService getCancelService() {
			return contained.getCancelService();
		}

		public void resetCancelService() {
			contained.resetCancelService();
		}	

		@Override
		public String toString() {
			return getName();
		}

		public static List<RoutingAlgorithmContainer> getRegisteredList() {
			List<RoutingAlgorithmContainer> list = new ArrayList<RoutingAlgorithmContainer>();
			for (RoutingAlgorithm ra : RoutingAlgorithm.getRegisteredList()) {
				list.add(new RoutingAlgorithmContainer(ra));
			}
			return list;
		}
	}

	private class RouteFind extends SwingWorker<Void, Void> implements Progress {

		private int maximum = 1;
		private int minimum = 0;
		private int value = 0;
		private int oldProgress = 0;

		@Override
		protected Void doInBackground() throws Exception {
			processRouteInner();
			return null;
		}

		@Override
		protected void done() {
			setUIEnabled(true);
			jProgress.setValue(0);
			jProgress.setIndeterminate(false);
		}

		@Override
		public int getMaximum() {
			return maximum;
		}

		@Override
		public void setMaximum(int maximum) {
			this.maximum = maximum;
		}

		@Override
		public int getMinimum() {
			return minimum;
		}

		@Override
		public void setMinimum(int minimum) {
			this.minimum = minimum;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public void setValue(int value) {
			this.value = value;
			int progress = (int) Math.floor(value * 100.0 / getMaximum());
			if (progress < 0) {
				progress = 0;
			}
			if (progress > 100) {
				progress = 100;
			}
			if (progress != oldProgress) {
				oldProgress = progress;
				setProgress(oldProgress);
			}
		}
	}

	static class SourceItem implements Comparable<SourceItem> {

		private final String name;
		private final boolean group;

		public SourceItem(final String name) {
			this.name = name;
			this.group = false;
		}

		public SourceItem(final String name, final boolean group) {
			this.name = name;
			this.group = group;
		}

		@Override
		public String toString() {
			if (group) {
				return TabsRouting.get().overviewGroup(name);
			} else {
				return name;
			}
		}

		public String getName() {
			return name;
		}

		@Override
		public int compareTo(final SourceItem o) {
			return this.getName().compareToIgnoreCase(o.getName());
		}
	}

	private static class JLoadMenuItem extends JMenuItem {

		private final Set<Long> systemIDs;

		public JLoadMenuItem(String name, Set<Long> systemIDs) {
			super(name, Images.FILTER_LOAD.getIcon());
			this.systemIDs = systemIDs;
		}

		public Set<Long> getSystemIDs() {
			return systemIDs;
		}
	}
}
