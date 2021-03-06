/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.gui.common.definitions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import jmt.engine.log.JSimLogger;
import jmt.engine.log.LoggerParameters;
import jmt.framework.data.BDMap;
import jmt.framework.data.BDMapImpl;
import jmt.framework.data.CachedHashMap;
import jmt.framework.data.MacroReplacer;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.classSwitch.ClassSwitch;
import jmt.gui.common.definitions.parametric.ParametricAnalysisDefinition;
import jmt.gui.common.forkStrategies.ClassSwitchFork;
import jmt.gui.common.forkStrategies.ForkStrategy;
import jmt.gui.common.forkStrategies.MultiBranchClassSwitchFork;
import jmt.gui.common.forkStrategies.OutPath;
import jmt.gui.common.joinStrategies.GuardJoin;
import jmt.gui.common.routingStrategies.LoadDependentRouting;
import jmt.gui.common.routingStrategies.ProbabilityRouting;
import jmt.gui.common.routingStrategies.RoutingStrategy;
import jmt.gui.common.transitionStrategies.TransitionVector;

/**
 * Created by IntelliJ IDEA.
 * User: OrsotronIII
 * Date: 24-mag-2005
 * Time: 9.46.43
 * Modified by Bertoli Marco
 * 
 * Modified by Francesco D'Aquino
 * 
 * Modified by Ashanka (May 2010):
 * Patch: Multi-Sink Perf. Index
 * Description: Added new Performance index for the capturing the 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 * Hence added a code to fetch the sinkkeys and if a measure is sink perf measure. 
 * 
 * Modified by Ashanka (June 2010):
 * Updated the Manage Probabilities. Closed Classes routed to Sink with probability <> 0.0 should show warning to Users.
 * 
 * Modified by Ashanka (July 2010)
 * Desc: Added new defaults control of a Random CheckBox.
 * 
 * Modified by Ashanka (Nov 2012)
 * Desc: Added refresh of LoadDependent Routing on deletion of station.
 */
public class CommonModel implements CommonConstants, ClassDefinition, StationDefinition, SimulationDefinition, BlockingRegionDefinition {

	//key generator
	long incrementalKey = 0;
	Long seed;
	Integer maxSamples;
	Double maxDuration;
	boolean useRandomSeed = true;
	Double maxSimulatedTime;
	double pollingInterval;
	protected MeasureDefinition results = null;
	protected Boolean disableStatistic = Boolean.FALSE;
	protected LoggerGlobalParameters loggerGlbParams;
	// Used to tell if model have to be saved
	protected boolean save = false;
	private static JSimLogger debugLog = JSimLogger.getRootLogger();;

	/**
	 * search keysets for classes, stations and measures
	 */
	protected Vector<Object> classesKeyset = new Vector<Object>();
	/**
	 * search keysets for classes, stations and measures
	 */
	protected Vector<Object> stationsKeyset = new Vector<Object>();
	/**
	 * search keysets for classes, stations and measures
	 */
	protected Vector<Object> noSourceSinkKeyset = new Vector<Object>();
	/**
	 * search keysets for classes, stations and measures
	 */
	protected Vector<Object> FJKeyset = new Vector<Object>();
	/**
	 * search keysets for classes, stations and measures
	 */
	protected Vector<Object> preloadableKeyset = new Vector<Object>();
	/**
	 * search keysets for classes, stations and measures
	 */
	protected Vector<Object> measuresKeyset = new Vector<Object>();
	/**
	 * search keysets for classes, stations and measures
	 */
	protected Vector<Object> blockingRegionsKeyset = new Vector<Object>();
	/**
	 * search keysets for classes, stations and measures
	 */
	protected Vector<Object> measuresBlockingKeyset = new Vector<Object>();
	/**
	 * search keysets for classes, stations and measures
	 */
	protected Vector<Object> blockableStations = new Vector<Object>();

	/**
	 * Hashmap for classes parameters
	 */
	protected HashMap<Object, Object> classDataHM = new CachedHashMap<Object, Object>();

	/**
	 * Hashmap containing station parameters
	 */
	protected HashMap<Object, Object> stationDataHM = new CachedHashMap<Object, Object>();

	/**
	 * Hashmap containing measure parameters
	 */
	protected HashMap<Object, MeasureData> measureDataHM = new CachedHashMap<Object, MeasureData>();

	/**
	 * Hashmap containing blocking region parameters
	 */
	protected HashMap<Object, Object> blockingDataHM = new CachedHashMap<Object, Object>();

	/**
	 * Hashmap containing measure parameters
	 */
	protected HashMap<Object, Object> blockingMeasureDataHM = new CachedHashMap<Object, Object>();

	/**
	 * BDMap containing station connections. On X coordinate will be put stationKeys for
	 * connection targets, on Y for sources.
	 */
	//protected BDMap connectionsBDM = new CachedBDMap(),
	protected BDMap connectionsBDM = new BDMapImpl();
	/**
	 * BDMap containing a set of station details for each station and each class.
	 * on X coordinate must be put class search keys, on Y station search keys.
	 */
	protected BDMap stationDetailsBDM = new BDMapImpl();
	/**
	 * BDMap containing a set of blocking region details for each blocking region and each class.
	 * on X coordinate must be put class search keys, on Y blocking region search keys.
	 */
	protected BDMap blockingDetailsBDM = new BDMapImpl();

	//S Jiang
	protected HashMap<Object, Vector<Object>> fakeForwardConnection = new CachedHashMap<Object, Vector<Object>>();
	//end

	// ------------------ Francesco D'Aquino ----------------------
	private ParametricAnalysisDefinition parametricAnalysisModel;
	private boolean parametricAnalysisEnabled;
	private boolean sinkProbabilityUpdate;//Probability Routing Error.
	private Vector<String> sinkProbabilityUpdateClasses = new Vector<String>();
	private Vector<String> sinkProbabilityUpdateStations = new Vector<String>();
	// -------------- end Francesco D'Aquino ----------------------

	/**
	 * Tells if current model is to be saved
	 * @return true if model is to be saved
	 */
	public boolean toBeSaved() {
		return save;
	}

	/**
	 * Resets save state. This MUST be called each time a model is saved
	 */
	public void resetSaveState() {
		save = false;
	}

	/**Creates a new instance of <code>CommonModel</code>*/
	public CommonModel() {
		seed = Defaults.getAsLong("simulationSeed");
		useRandomSeed = Defaults.getAsBoolean("isSimulationSeedRandom").booleanValue();
		maxDuration = Defaults.getAsDouble("simulationMaxDuration");
		maxSimulatedTime = Defaults.getAsDouble("maxSimulatedTime");
		maxSamples = Defaults.getAsInteger("maxSimulationSamples");
		pollingInterval = Defaults.getAsDouble("simulationPolling").doubleValue();
		//TODO: eventually load the parametricAnalysisEnabled state from default
		parametricAnalysisEnabled = false;
	}

	//model class definition methods
	/**
	 * This method returns the entire set of class keys.
	 */
	@Override
	public synchronized Vector<Object> getClassKeys() {
		return classesKeyset;
	}

	/**
	 * This method returns the set of open class keys.
	 *
	 * Author: Francesco D'Aquino
	 */
	@Override
	public Vector<Object> getOpenClassKeys() {
		Vector<Object> classes = this.getClassKeys();
		Vector<Object> openClassKeys = new Vector<Object>(0, 1);
		for (int i = 0; i < classes.size(); i++) {
			Object thisClass = classes.get(i);
			if (this.getClassType(thisClass) == CommonConstants.CLASS_TYPE_OPEN) {
				openClassKeys.add(thisClass);
			}
		}
		return openClassKeys;
	}

	/**
	 * This method returns the sum of population of each close class
	 *
	 * @return the total close class population
	 *
	 * Author: Francesco D'Aquino
	 */
	@Override
	public int getTotalCloseClassPopulation() {
		int sum = 0;
		Vector<Object> closeClasses = getClosedClassKeys();
		for (int i = 0; i < closeClasses.size(); i++) {
			sum += getClassPopulation(closeClasses.get(i)).intValue();
		}
		return sum;
	}

	/**
	 * This method returns the set of closed class keys.
	 */
	@Override
	public synchronized Vector<Object> getClosedClassKeys() {
		Vector<Object> closedClasses = new Vector<Object>(0, 1);
		for (int i = 0; i < classesKeyset.size(); i++) {
			Object thisClassKey = classesKeyset.get(i);
			int classType = getClassType(thisClassKey);
			if (classType == CommonConstants.CLASS_TYPE_CLOSED) {
				closedClasses.add(thisClassKey);
			}
		}
		return closedClasses;
	}

	/**
	 * Returns name of the class linked to the specific key
	 * */
	@Override
	public synchronized String getClassName(Object key) {
		if (classesKeyset.contains(key)) {
			return ((ClassData) classDataHM.get(key)).name;
		} else {
			return null;
		}
	}

	/**
	 * Sets name of the class linked to the specific key
	 * */
	@Override
	public synchronized void setClassName(String name, Object key) {
		if (classDataHM.containsKey(key)) {
			String oldName = ((ClassData) classDataHM.get(key)).name;
			((ClassData) classDataHM.get(key)).name = name;
			if (!oldName.equals(name)) {
				save = true;
			}
		}
	}

	/**
	 * Returns type of the class linked to the specific key. Type of class is represented by
	 * an int number whose value is contained in <code>JSIMConstants</code>.
	 * */
	@Override
	public synchronized int getClassType(Object key) {
		if (classesKeyset.contains(key)) {
			return ((ClassData) classDataHM.get(key)).type;
		} else {
			return -1;
		}
	}

	/**
	 * Sets type of the class linked to the specific key. Type of class is represented by
	 * an int number whose value is contained in <code>JSIMConstants</code>.
	 * */
	@Override
	public synchronized void setClassType(int type, Object key) {
		//Must check for class presence
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = ((ClassData) classDataHM.get(key));
		} else {
			return;
		}
		int classTypeInt = cd.type;
		//if new type is different from the former one, population and distribution
		//values are senseless, so they must be erased both
		if (classTypeInt != type) {
			save = true;
			cd.distribution = cd.population = null;
			cd.type = type;
			if (type == CLASS_TYPE_OPEN) {
				cd.distribution = Defaults.getAsNewInstance("classDistribution");
			} else if (type == CLASS_TYPE_CLOSED) {
				cd.population = Defaults.getAsInteger("classPopulation");
				cd.refSource = null;
			}
		}
		for (Object station: this.getStationKeys()) {
			for (Object classK: this.getClassKeys()) {
				Object strategy = this.getForkingStrategy(station, classK);
				if (strategy instanceof MultiBranchClassSwitchFork) {
					Map<Object, OutPath> m = ((MultiBranchClassSwitchFork) strategy).getOutDetails();
					for (OutPath o: m.values()) {
						o.getOutParameters().put(key, 0);
					}
				}
				if (strategy instanceof ClassSwitchFork) {
					Map<Object, OutPath> m = ((ClassSwitchFork) strategy).getOutDetails();
					for (OutPath o: m.values()) {
						o.getOutParameters().put(key, 0);
					}
				}
				strategy = this.getJoinStrategy(station, classK);
				if (strategy instanceof GuardJoin) {
					((GuardJoin) strategy).getGuard().put(key, 0);
				}
				/*strategy = this.getSemaphoreStrategy(station, classK);
				if (strategy instanceof NormalSemaphore) {
					((NormalSemaphore) strategy).getSemaphore().put(key, 0);
				}*/
			}
		}
	}

	/**
	 * Returns name of the class linked to the specific key
	 * */
	@Override
	public synchronized int getClassPriority(Object key) {
		if (classesKeyset.contains(key)) {
			return ((ClassData) classDataHM.get(key)).priority;
		} else {
			return -1;
		}
	}

	/**
	 * Sets name of the class linked to the specific key
	 * */
	@Override
	public synchronized void setClassPriority(int priority, Object key) {
		if (classDataHM.containsKey(key)) {
			int oldPriority = ((ClassData) classDataHM.get(key)).priority;
			((ClassData) classDataHM.get(key)).priority = priority;
			if (oldPriority != priority) {
				save = true;
			}
		}
	}

	/**
	 * Returns population of the class linked to the specific key. Return value is an integer
	 * if specified class is a closed class, null, otherwise.
	 * */
	@Override
	public synchronized Integer getClassPopulation(Object key) {
		if (classDataHM.containsKey(key)) {
			return ((ClassData) classDataHM.get(key)).population;
		} else {
			return null;
		}
	}

	/**
	 * Sets population of the class linked to the specific key
	 * */
	@Override
	public synchronized void setClassPopulation(Integer population, Object key) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return;
		}
		if (cd.type == CLASS_TYPE_CLOSED) {
			if (!cd.population.equals(population)) {
				save = true;
			}
			cd.population = population;
		} else {
			cd.population = null;
		}
	}

	/**
	 * Returns inter-arrival time distribution of the class linked to the specific key.
	 * Return value is an object representing distribution if specified class is an open
	 *  class and reference station is not class switch, null otherwise.
	 * */
	@Override
	public synchronized Object getClassDistribution(Object key) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			ClassData classData = ((ClassData) classDataHM.get(key));
			if (classData.refSource != null
					&& (classData.refSource.equals(STATION_TYPE_CLASSSWITCH)
							|| classData.refSource.equals(STATION_TYPE_FORK)
							|| classData.refSource.equals(STATION_TYPE_SCALER))) {
				return null;
			}
			cd = (ClassData) classDataHM.get(key);
		} else {
			return null;
		}
		return cd.distribution;
	}

	/**
	 * Sets inter-arrival time distribution of the class linked to the specific key
	 * */
	@Override
	public synchronized void setClassDistribution(Object distribution, Object key) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return;
		}
		if (cd.type == CLASS_TYPE_OPEN) {
			if (!cd.distribution.equals(distribution)) {
				save = true;
			}
			cd.distribution = distribution;
		} else {
			cd.population = null;
		}
	}

	/**Returns a class parameter, given the class key of search, and the parameter name
	 * specified by proper code.*/
	@Override
	public synchronized Object getClassParameter(Object key, int parameterName) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return null;
		}
		switch (parameterName) {
		case CLASS_NAME: {
			return cd.name;
		}
		case CLASS_TYPE: {
			return new Integer(cd.type);
		}
		case CLASS_PRIORITY: {
			return new Integer(cd.priority);
		}
		case CLASS_POPULATION: {
			return cd.population;
		}
		case CLASS_DISTRIBUTION: {
			return cd.distribution;
		}
		default: {
			return null;
		}
		}
	}

	/**Sets a class parameter, given the class key of search, and the parameter name
	 * specified by proper code. User is responsible for passing correct type for
	 * parameters, e.g. name must be a String, type, priority and population must be an
	 * integer. Distribution must be an object. If type for a field is not correct, a
	 * <code> ClassCastException</code> will be raised.*/
	@Override
	public synchronized void setClassParameter(Object key, int parameterName, Object value) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return;
		}
		switch (parameterName) {
		case CLASS_NAME: {
			cd.name = (String) value;
			break;
		}
		case CLASS_TYPE: {
			cd.type = ((Integer) value).intValue();
			break;
		}
		case CLASS_PRIORITY: {
			cd.priority = ((Integer) value).intValue();
			break;
		}
		case CLASS_POPULATION: {
			cd.population = (Integer) value;
			break;
		}
		case CLASS_DISTRIBUTION: {
			cd.distribution = value;
			break;
		}
		}
		save = true;
	}

	/**Sets all class parameters at once given the class' search key. If search key does
	 * not exist, no class will be added.*/
	@Override
	public synchronized void setClassParameters(Object key, String name, int type, int priority, Integer population, Object distribution) {
		if (!classesKeyset.contains(key)) {
			return;
		}
		if (type == CLASS_TYPE_CLOSED) {
			distribution = null;
		} else if (type == CLASS_TYPE_OPEN) {
			population = null;
		}
		classDataHM.put(key, new ClassData(name, type, priority, population, distribution));
		save = true;
	}

	/**Adds a class and sets all class parameters at once, return the new search key
	 * identifying the new class.*/
	@Override
	public synchronized Object addClass(String name, int type, int priority, Integer population, Object distribution) {
		//generating a new key
		incrementalKey++;
		Object key = new Long(incrementalKey);
		classesKeyset.add(key);

		if (type == CLASS_TYPE_CLOSED) {
			distribution = null;
		} else if (type == CLASS_TYPE_OPEN) {
			population = null;
		}
		//add class to model
		classDataHM.put(key, new ClassData(name, type, priority, population, distribution));
		//add per-class station details
		addStationDetailsForClass(key);
		//add per-class blocking region details
		addBlockingDetailsForClass(key);
		for (Object station: this.getStationKeys()) {
			for (Object classK: this.getClassKeys()) {
				Object strategy = this.getForkingStrategy(station, classK);
				if (strategy instanceof MultiBranchClassSwitchFork) {
					Map<Object, OutPath> m = ((MultiBranchClassSwitchFork) strategy).getOutDetails();
					for (OutPath o: m.values()) {
						o.getOutParameters().put(key, 0);
					}
				}
				if (strategy instanceof ClassSwitchFork) {
					Map<Object, OutPath> m = ((ClassSwitchFork) strategy).getOutDetails();
					for (OutPath o: m.values()) {
						o.getOutParameters().put(key, 0);
					}
				}
				strategy = this.getJoinStrategy(station, classK);
				if (strategy instanceof GuardJoin) {
					((GuardJoin) strategy).getGuard().put(key, 0);
				}
				/*strategy = this.getSemaphoreStrategy(station, classK);
				if (strategy instanceof NormalSemaphore) {
					((NormalSemaphore) strategy).getSemaphore().put(key, 0);
				}*/
			}
		}
		save = true;
		return key;
	}

	/**Deletes class given its search key. */
	@Override
	public synchronized void deleteClass(Object key) {
		if (classesKeyset.contains(key)) {
			classesKeyset.remove(key);
			classDataHM.remove(key);
			deleteStationDetailsForClass(key);
			deleteBlockingDetailsForClass(key);
			save = true;
		}
	}

	/**
	 * Sets the reference station for a given class
	 * @param classKey search key for class
	 * @param stationKey search key for referenced station
	 */
	@Override
	public void setClassRefStation(Object classKey, Object stationKey) {
		ClassData cd = (ClassData) classDataHM.get(classKey);
		if (cd == null) {
			return;
		}
		Object oldRef = cd.refSource;
		if (CommonConstants.STATION_TYPE_CLASSSWITCH.equals(stationKey))
			cd.refSource = CommonConstants.STATION_TYPE_CLASSSWITCH;
		else if (CommonConstants.STATION_TYPE_FORK.equals(stationKey))
			cd.refSource = CommonConstants.STATION_TYPE_FORK;
		else if (CommonConstants.STATION_TYPE_SCALER.equals(stationKey))
			cd.refSource = CommonConstants.STATION_TYPE_SCALER;
		else
			cd.refSource = stationsKeyset.contains(stationKey) ? stationKey : null;
		if (oldRef != stationKey) {
			save = true;
		}
	}

	/**
	 * Returns the reference station for a given class
	 * @param classKey search key for class
	 * @return search key for referenced station or null if it was not specified yet
	 */
	@Override
	public Object getClassRefStation(Object classKey) {
		ClassData cd = (ClassData) classDataHM.get(classKey);
		if (cd == null) {
			return null;
		}
		//if previously saved refSource has been deleted, delete it to maintain coherence
		if (!stationsKeyset.contains(cd.refSource)
				&& !CommonConstants.STATION_TYPE_CLASSSWITCH.equals(cd.refSource)
				&& !CommonConstants.STATION_TYPE_FORK.equals(cd.refSource)
				&& !CommonConstants.STATION_TYPE_SCALER.equals(cd.refSource)) { 
			cd.refSource = null;
		}
		return cd.refSource;
	}

	/*-------------------------------------------------------------------------------
	 *----------------------  methods for station definition  -----------------------
	 *-------------------------------------------------------------------------------*/

	/**
	 * This method returns the key set of sources
	 *
	 * @return an array containing the entire set of source keys
	 *
	 * Author: Francesco D'Aquino
	 */
	@Override
	public Vector<Object> getStationKeysSource() {
		Vector<Object> stations = this.getStationKeys();
		Vector<Object> sources = new Vector<Object>(0, 1);
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			if (this.getStationType(thisStation).equals(CommonConstants.STATION_TYPE_SOURCE)) {
				sources.add(thisStation);
			}
		}
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			if (this.getStationType(thisStation).equals(CommonConstants.STATION_TYPE_CLASSSWITCH)) {
				sources.add(CommonConstants.STATION_TYPE_CLASSSWITCH);
				break;
			}
		}
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			if (this.getStationType(thisStation).equals(CommonConstants.STATION_TYPE_FORK)) {
				sources.add(CommonConstants.STATION_TYPE_FORK);
				break;
			}
		}
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			if (this.getStationType(thisStation).equals(CommonConstants.STATION_TYPE_SCALER)) {
				sources.add(CommonConstants.STATION_TYPE_SCALER);
				break;
			}
		}
		return sources;
	}

	/**
	 * This method returns the key set of servers
	 *
	 * @return an array containing the entire set of server keys
	 *
	 * Author: Francesco D'Aquino
	 */
	@Override
	public Vector<Object> getStationKeysServer() {
		Vector<Object> stations = this.getStationKeys();
		Vector<Object> servers = new Vector<Object>(0, 1);
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			if (this.getStationType(thisStation).equals(CommonConstants.STATION_TYPE_SERVER)) {
				servers.add(thisStation);
			}
		}
		return servers;
	}

	/**
	 * This method returns the key set of delays
	 *
	 * @return an array containing the entire set of delay keys
	 *
	 * Author: Francesco D'Aquino
	 */
	@Override
	public Vector<Object> getStationKeysDelay() {
		Vector<Object> stations = this.getStationKeys();
		Vector<Object> delays = new Vector<Object>(0, 1);
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			if (this.getStationType(thisStation).equals(CommonConstants.STATION_TYPE_DELAY)) {
				delays.add(thisStation);
			}
		}
		return delays;
	}

	/**
	 * This method returns the key set of sinks
	 *
	 * @return an array containing the entire set of sink keys
	 *
	 * Author: Francesco D'Aquino
	 */
	@Override
	public Vector<Object> getStationKeysSink() {
		Vector<Object> stations = this.getStationKeys();
		Vector<Object> sinks = new Vector<Object>(0, 1);
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			if (this.getStationType(thisStation).equals(CommonConstants.STATION_TYPE_SINK)) {
				sinks.add(thisStation);
			}
		}
		return sinks;
	}

	/**
	 * This method returns the key set of loggers
	 *
	 * @return an array containing the entire set of logger keys
	 *
	 * Author: Michael Fercu (Marco Bertoli) 0.7.4
	 */
	@Override
	public Vector<Object> getStationKeysLogger() {
		Vector<Object> stations = this.getStationKeys();
		Vector<Object> loggers = new Vector<Object>(0, 1);
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			if (this.getStationType(thisStation).equals(CommonConstants.STATION_TYPE_LOGGER)) {
				loggers.add(thisStation);
			}
		}
		return loggers;
	}

	/**
	 * This method returns the key set of places
	 *
	 * @return an array containing the entire set of place keys
	 *
	 * Author: Lulai Zhu
	 */
	@Override
	public Vector<Object> getStationKeysPlace() {
		Vector<Object> stations = this.getStationKeys();
		Vector<Object> places = new Vector<Object>(0, 1);
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			if (this.getStationType(thisStation).equals(CommonConstants.STATION_TYPE_PLACE)) {
				places.add(thisStation);
			}
		}
		return places;
	}

	/**
	 * This method returns the key set of transitions
	 *
	 * @return an array containing the entire set of transition keys
	 *
	 * Author: Lulai Zhu
	 */
	@Override
	public Vector<Object> getStationKeysTransition() {
		Vector<Object> stations = this.getStationKeys();
		Vector<Object> transitions = new Vector<Object>(0, 1);
		for (int i = 0; i < stations.size(); i++) {
			Object thisStation = stations.get(i);
			if (this.getStationType(thisStation).equals(CommonConstants.STATION_TYPE_TRANSITION)) {
				transitions.add(thisStation);
			}
		}
		return transitions;
	}

	/**Returns entire key set for search of stations. Objects contained  in returned vector
	 * can be directly passed to methods for parameters retrieval.*/
	@Override
	public Vector<Object> getStationKeys() {
		return stationsKeyset;
	}

	/**
	 * This method returns all station keys without source and sink ones.
	 */
	@Override
	public Vector<Object> getStationKeysNoSourceSink() {
		return noSourceSinkKeyset;
	}

	/**
	 * This method returns all fork/join keys.
	 */
	@Override
	public Vector<Object> getFJKeys() {
		return FJKeyset;
	}

	/**
	 * This method returns all preloadable station keys.
	 */
	@Override
	public Vector<Object> getStationKeysPreloadable() {
		return preloadableKeyset;
	}

	/**
	 * This method returns all blocking region keys.
	 */
	@Override
	public Vector<Object> getFCRegionKeys() {
		return blockingRegionsKeyset;
	}

	/**
	 * This method returns all station (except sources and sinks) and blocking region keys.
	 */
	@Override
	public Vector<Object> getStationRegionKeysNoSourceSink() {
		Vector<Object> vect = new Vector<Object>(noSourceSinkKeyset);
		vect.addAll(blockingRegionsKeyset);
		return vect;
	}

	/** Returns name of the station in <code>String</code> representation, given the
	 * search key.
	 * @param key: search key for station to be modified
	 * @return : name of station.*/
	@Override
	public synchronized String getStationName(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.name;
	}

	/** Sets type of the station, given the search key.
	 * @param key: search key for station to be modified
	 * @param name: name of station.*/
	@Override
	public synchronized void setStationName(String name, Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		if (sd.name != name) {
			save = true;
		}
		sd.name = name;
	}

	/** Returns queue capacity of the station, given the search key.*/
	@Override
	public synchronized Integer getStationQueueCapacity(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.queueCapacity;
	}

	/** Sets queue capacity of the station, given the search key.*/
	@Override
	public synchronized void setStationQueueCapacity(Integer queueCapacity, Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		if (!sd.queueCapacity.equals(queueCapacity)) {
			save = true;
		}
		sd.queueCapacity = queueCapacity;
	}

	/** Returns number of servers or number of forked job for the station given the search key.*/
	@Override
	public synchronized Integer getStationNumberOfServers(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.numOfServers;
	}

	/** Sets number of servers or number of forked job for the station, given the search key.*/
	@Override
	public synchronized void setStationNumberOfServers(Integer numberOfServers, Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		if (!sd.numOfServers.equals(numberOfServers)) {
			save = true;
		}
		sd.numOfServers = numberOfServers;
	}

	/** Returns parameter for the station given the search key and parameter code.*/
	@Override
	public synchronized Object getStationParameter(Object key, int parameterCode) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		switch (parameterCode) {
		case STATION_NAME: {
			return sd.name;
		}
		case STATION_TYPE: {
			return sd.type;
		}
		case STATION_QUEUE_CAPACITY: {
			return sd.queueCapacity;
		}
		case STATION_NUMBER_OF_SERVERS: {
			return sd.numOfServers;
		}
		default: {
			return null;
		}
		}
	}

	/** Sets parameter for the station, given the search key and parameter code.*/
	@Override
	public synchronized void setStationParameter(Object value, Object key, int parameterCode) {
		switch (parameterCode) {
		case STATION_NAME: {
			setStationName((String) value, key);
			break;
		}
		case STATION_TYPE: {
			setStationType((String) value, key);
			break;
		}
		case STATION_QUEUE_CAPACITY: {
			setStationQueueCapacity((Integer) value, key);
			break;
		}
		case STATION_NUMBER_OF_SERVERS: {
			setStationNumberOfServers((Integer) value, key);
			break;
		}
		}
		save = true;
	}

	/** Returns type of the station in <code>String</code> representation, given the
	 * search key.
	 * @param key: search key for station to be modified
	 * @return : type of station.*/
	@Override
	public synchronized String getStationType(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.type;
	}

	/** Sets name of the station, given the search key.
	 * @param key: search key for station to be modified
	 * @param type: type of station.*/
	@Override
	public synchronized void setStationType(String type, Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		String oldType = sd.type;
		sd.type = type;
		//If type has changed, all of the parameters must be reset to defaults
		if (!oldType.equals(sd.type)) {
			save = true;
			setDefaultsForStation(sd);
			deleteStationConnections(key);
			addNewStationConnections(key);
			addNewStationDetails(key);
			// If new type is not source or sink add to noSourceSink
			if (!type.equals(STATION_TYPE_SOURCE) && !type.equals(STATION_TYPE_SINK)) {
				if (!noSourceSinkKeyset.contains(key)) {
					noSourceSinkKeyset.add(key);
				}
			} else {
				noSourceSinkKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_FORK) || type.equals(STATION_TYPE_SCALER)) {
				if (!FJKeyset.contains(key)) {
					FJKeyset.add(key);
				}
			} else {
				FJKeyset.remove(key);
			}
			if (isStationTypePreloadable(type)) {
				if (!preloadableKeyset.contains(key)) {
					preloadableKeyset.add(key);
				}
			} else {
				preloadableKeyset.remove(key);
			}
			// If station is in a blocking region removes it if its type is not valid
			if (sd.blockingRegion != null) {
				if (!canStationTypeBeBlocked(sd.type)) {
					this.removeRegionStation(sd.blockingRegion, key);
				}
			} else {
				if (canStationTypeBeBlocked(sd.type) && !blockableStations.contains(key)) {
					blockableStations.add(key);
				} else if (!canStationTypeBeBlocked(sd.type)) {
					blockableStations.remove(key);
				}
			}
		}
	}

	/*sets default values for station parameters. To be used when type of station has
	 * been changed*/
	private void setDefaultsForStation(StationData sd) {
		sd.queueStrategy = QUEUE_STRATEGY_STATION_QUEUE;
		if (sd.type.equals(STATION_TYPE_SERVER)) {
			sd.queueStrategy = Defaults.get("stationStationQueueStrategy");
		} else if (sd.type.equals(STATION_TYPE_FORK)) {
			sd.queueStrategy = Defaults.get("stationStationQueueStrategy");
			if (sd.queueStrategy.equals(QUEUE_STRATEGY_STATION_PS) || sd.queueStrategy.equals(QUEUE_STRATEGY_STATION_SRPT)) {
				sd.queueStrategy = QUEUE_STRATEGY_STATION_QUEUE;
			}
		}

		sd.numOfServers = Defaults.getAsInteger("stationServers");
		sd.queueCapacity = Defaults.getAsInteger("stationCapacity");

		if (sd.type.equals(STATION_TYPE_DELAY)) {
			/*Delay stations have infinite number of servers and no queue*/
			sd.numOfServers = new Integer(-1);
		} else if (sd.type.equals(STATION_TYPE_SOURCE) || sd.type.equals(STATION_TYPE_SINK) || sd.type.equals(STATION_TYPE_TERMINAL)
				|| sd.type.equals(STATION_TYPE_ROUTER) || sd.type.equals(STATION_TYPE_JOIN) || sd.type.equals(STATION_TYPE_LOGGER)
				|| sd.type.equals(STATION_TYPE_SEMAPHORE) || sd.type.equals(STATION_TYPE_PLACE)) {
			/*If this station has no queue and no service section, there's no need to
			 * set any parameter value for these sections.*/
			sd.numOfServers = null;
		} else if (sd.type.equals(STATION_TYPE_FORK) || sd.type.equals(STATION_TYPE_SCALER)) {
			sd.forkBlock = Defaults.getAsInteger("forkBlock");
			sd.numOfServers = Defaults.getAsInteger("forkJobsPerLink");
		} else if (sd.type.equals(STATION_TYPE_TRANSITION)) {
			sd.numOfServers = Defaults.getAsInteger("transitionServerNumber");
		}

		if (sd.type.equals(STATION_TYPE_PLACE)) {
			sd.queueCapacity = Defaults.getAsInteger("storeCapacity");
		}
	}

	/**Adds a new station to the model. Name and type must be specified.
	 * @param name: name of the new station
	 * @param type: string representing station type. it is value is contained in
	 * <code>JSIMConstants</code> interface.
	 * @return : key of search for this class*/
	@Override
	public synchronized Object addStation(String name, String type) {
		Object key = new Long(++incrementalKey);
		//If this station has already been created, do not add search key to the list.
		if (!stationsKeyset.contains(key)) {
			stationsKeyset.add(key);
			if (!type.equals(STATION_TYPE_SOURCE) && !type.equals(STATION_TYPE_SINK)) {
				noSourceSinkKeyset.add(key);
			}
			if (type.equals(STATION_TYPE_FORK) || type.equals(STATION_TYPE_SCALER)) {
				FJKeyset.add(key);
			}
			if (isStationTypePreloadable(type)) {
				preloadableKeyset.add(key);
			}
			// If this station can be blocked, adds it to blockable stations vector
			if (canStationTypeBeBlocked(type)) {
				blockableStations.add(key);
			}
		}
		StationData newStation = new StationData(name, type, null, null);
		setDefaultsForStation(newStation);
		//add station to the correspondent hashmap
		stationDataHM.put(key, newStation);
		//setup connection defaults
		addNewStationConnections(key);
		//setup station details
		addNewStationDetails(key);
		save = true;
		//return key
		return key;
	}

	/**Deletes station given a search key.*/
	@Override
	public synchronized void deleteStation(Object key) {
		if (stationsKeyset.contains(key)) {
			// Removes this station from owner blocking region (if needed)
			StationData sd = (StationData) stationDataHM.get(key);
			//Part where LoadDependant Routing must be refreshed:
			Vector<Object> bacwardStationKeys = this.getBackwardConnections(key);
			for (Iterator<Object> it = bacwardStationKeys.iterator(); it.hasNext();) {
				Object stationKey = it.next();
				Vector<Object> classKeys = this.getClassKeys();
				for (Object classKey : classKeys) {
					RoutingStrategy rs = (RoutingStrategy)this.getRoutingStrategy(stationKey, classKey);
					ForkStrategy fs = (ForkStrategy) this.getForkingStrategy(stationKey, classKey);
					if (rs instanceof LoadDependentRouting) {
						LoadDependentRouting ld = (LoadDependentRouting) rs;
						ld.refreshRoutingOnStationDeletion(this.getStationName(key));
					}
					if (fs != null) {
						fs.removeStation(key);
					}
				}
			}

			if (sd.blockingRegion != null) {
				removeRegionStation(sd.blockingRegion, key);
			}
			stationsKeyset.remove(key);
			stationDataHM.remove(key);
			deleteStationConnections(key);
			deleteStationDetails(key);
			deleteStationMeasures(key);
			noSourceSinkKeyset.remove(key);
			FJKeyset.remove(key);
			preloadableKeyset.remove(key);
			blockableStations.remove(key);
			save = true;
		}

	}

	// check if use simplified fork strategy
	@Override
	public Boolean getIsSimplifiedFork(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.isSimplifiedFork;
	}

	@Override
	public void setIsSimplifiedFork(Object key, Boolean value) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		if (!sd.isSimplifiedFork.equals(value)) {
			save = true;
		}
		sd.isSimplifiedFork = value;
	}

	/**
	 * Tells if a fork is blocking
	 * <br>Author: Bertoli Marco
	 * @param key search's key for fork
	 * @return maximum number of jobs allowed in a fork-join
	 * region (-1 is infinity)
	 */
	@Override
	public Integer getForkBlock(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.forkBlock;
	}

	/**
	 * Sets if a fork is blocking
	 * <br>Author: Bertoli Marco
	 * @param key search's key for fork
	 * @param value maximum number of jobs allowed in a fork-join
	 * region (-1 is infinity)
	 */
	@Override
	public void setForkBlock(Object key, Integer value) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		if (!sd.forkBlock.equals(value)) {
			save = true;
		}
		sd.forkBlock = value;
	}

	/*------------------------------------------------------------------------------
	 *---------------- Methods for setup of class-station parameters ---------------
	 *------------------------------------------------------------------------------*/

	/**
	 * Returns drop rule associated with given station queue section if capacity is finite
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return FINITE_DROP || FINITE_BLOCK || FINITE_WAITING
	 */
	@Override
	public String getDropRule(Object stationKey, Object classKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null) {
			return current.dropRule;
		} else {
			return null;
		}
	}

	/**
	 * Sets drop rule associated with given station queue section if capacity is finite
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param rule: FINITE_DROP || FINITE_BLOCK || FINITE_WAITING
	 */
	@Override
	public void setDropRule(Object stationKey, Object classKey, String rule) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null && rule != null) {
			if (!current.dropRule.equals(rule)) {
				save = true;
			}
			current.dropRule = rule;
		}
	}

	@Override
	public void setStationQueueStrategy(Object stationKey, String strategy) {
		StationData station = (StationData) stationDataHM.get(stationKey);
		if (station != null) {
			if (!station.queueStrategy.equals(strategy)) {
				save = true;
			}
			station.queueStrategy = strategy;
		}
	}

	@Override
	public String getStationQueueStrategy(Object stationKey) {
		StationData station = (StationData) stationDataHM.get(stationKey);
		if (station != null) {
			return station.queueStrategy;
		} else {
			return null;
		}
	}

	@Override
	public void setQueueStrategy(Object stationKey, Object classKey, String queueStrategy) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null) {
			if (!current.queueStrategy.equals(queueStrategy)) {
				save = true;
			}
			current.queueStrategy = queueStrategy;
		}
	}

	@Override
	public String getQueueStrategy(Object stationKey, Object classKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null) {
			return current.queueStrategy;
		} else {
			return null;
		}
	}

	@Override
	public void setServiceTimeDistribution(Object stationKey, Object classKey, Object distribution) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null) {
			if (!distribution.equals(current.serviceDistribution)) {
				save = true;
			}
			current.serviceDistribution = distribution;
		}
	}

	@Override
	public Object getServiceTimeDistribution(Object stationKey, Object classKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null) {
			return current.serviceDistribution;
		} else {
			return null;
		}
	}

	@Override
	public void setRoutingStrategy(Object stationKey, Object classKey, Object routingStrategy) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null) {
			if (!routingStrategy.equals(current.routingStrategy)) {
				save = true;
			}
			current.routingStrategy = routingStrategy;
		}
	}

	@Override
	public void setForkingStrategy(Object stationKey, Object classKey,
			Object forkingStrategy) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(
				classKey, stationKey);
		if (current != null) {
			if (!forkingStrategy.equals(current.forkingStrategy)) {
				save = true;
			}
			current.forkingStrategy = forkingStrategy;
		}
	}

	@Override
	public void setJoinStrategy(Object stationKey, Object classKey,
			Object joinStrategy) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(
				classKey, stationKey);
		if (current != null) {
			//if (!joinStrategy.equals(current.joinStrategy)) {
			save = true;
			//}
			current.joinStrategy = joinStrategy;
		}
	}

	@Override
	public void setSemaphoreStrategy(Object stationKey, Object classKey,
			Object semaphoreStrategy) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(
				classKey, stationKey);
		if (current != null) {
			//if (!semaphoreStrategy.equals(current.semaphoreStrategy)) {
			save = true;
			//}
			current.semaphoreStrategy = semaphoreStrategy;
		}
	}

	@Override
	public Object getRoutingStrategy(Object stationKey, Object classKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null) {
			return current.routingStrategy;
		} else {
			return null;
		}
	}

	@Override
	public Object getForkingStrategy(Object stationKey, Object classKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(
				classKey, stationKey);
		if (current != null) {
			return current.forkingStrategy;
		} else {
			return null;
		}
	}

	@Override
	public Object getJoinStrategy(Object stationKey, Object classKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(
				classKey, stationKey);
		if (current != null) {
			return current.joinStrategy;
		} else {
			return null;
		}
	}

	@Override
	public Object getSemaphoreStrategy(Object stationKey, Object classKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(
				classKey, stationKey);
		if (current != null) {
			return current.semaphoreStrategy;
		} else {
			return null;
		}
	}

	/**
	 * Michael Fercu (Marco Bertoli) - 0.7.4
	 * Access methods for "Logger"
	 */
	@Override
	public void setLoggingParameters(Object stationKey, Object loggerParameters) {
		StationData sd;
		if (stationDataHM.containsKey(stationKey)) {
			sd = (StationData) stationDataHM.get(stationKey);
		} else {
			return;
		}

		if (sd.loggerParameters == null) {
			save = true;
		} else if (!sd.loggerParameters.equals(loggerParameters)) {
			save = true;
		}

		sd.loggerParameters = loggerParameters;
	}

	@Override
	public Object getLoggingParameters(Object stationKey) {
		Object loggerParams = null;
		if (stationDataHM.containsKey(stationKey)) {
			StationData sd = (StationData) stationDataHM.get(stationKey);
			loggerParams = (sd.loggerParameters != null) ? sd.loggerParameters : new LoggerParameters();
		} else {
			return null;
		}
		return loggerParams;
	}

	@Override
	public String getLoggingGlbParameter(String selector) {
		// The global parameters must be instantiated for a new file
		if (loggerGlbParams == null) {
			loggerGlbParams = new LoggerGlobalParameters();
		}

		if (selector.equalsIgnoreCase("path") == true) {
			return loggerGlbParams.path;
		} else if (selector.equalsIgnoreCase("delim") == true) {
			return loggerGlbParams.delimiter;
		} else if (selector.equalsIgnoreCase("decimalSeparator") == true) {
			return loggerGlbParams.decimalSeparator;
		} else if (selector.equalsIgnoreCase("autoAppend") == true) {
			return loggerGlbParams.autoAppendMode.toString();
		} else {
			debugLog.error("No such selector " + selector + " for " + new Exception().getStackTrace()[0] + "\n" + new Exception().getStackTrace()[1]);
		}
		return null;
	}

	@Override
	public void setLoggingGlbParameter(String selector, String value) {
		// The global parameters must be instantiated for a new file
		if (loggerGlbParams == null) {
			loggerGlbParams = new LoggerGlobalParameters();
		}

		// debugLog.debug("glbParameter <" + selector + "=" + value + "> assigned by " + (new Exception().getStackTrace()[1]).toString().substring(19));
		debugLog.debug("glbParameter <" + selector + "=" + value + ">");

		if (selector.equalsIgnoreCase("path") == true) {
			this.loggerGlbParams.path = value;
		} else if (selector.equalsIgnoreCase("decimalSeparator") == true) {
			this.loggerGlbParams.decimalSeparator = value;
		} else if (selector.equalsIgnoreCase("delim") == true) {
			this.loggerGlbParams.delimiter = value;
		} else if (selector.equalsIgnoreCase("autoAppend") == true) {
			this.loggerGlbParams.autoAppendMode = new Integer(value);
		} else {
			debugLog.error("No such selector " + selector + " for " + new Exception().getStackTrace()[0] + "\n" + new Exception().getStackTrace()[1]);
			return;
		}

		save = true;
	}

	/**
	 * Used by jmodel.controller.Mediator to get the <i>files that could be overwritten</i>,
	 * in order to warn the user with a dialog box. 
	 * @author MF08 0.7.4 - Michael Fercu for Logger (Marco Bertoli)
	 * @return String[] of the filename of all log files that will be written to.
	 */
	public String[] getLoggerNameList() {
		Vector<Object> loggerkeys = getStationKeysLogger();
		int loggerkeyssize = loggerkeys.size();
		String[] lknames = new String[loggerkeyssize];
		String[] lknames2;
		int size = 0;
		boolean globalWasProcessed = false;

		// get the list of all filenames from their keys
		for (int i = 0; i < loggerkeyssize; i++) {
			LoggerParameters lp = (LoggerParameters) getLoggingParameters(loggerkeys.get(i));
			if (lp.name.equalsIgnoreCase("global") == false) {
				lknames[size++] = lp.name;
			} else if ((lp.name.equalsIgnoreCase("global") == true) && (globalWasProcessed == false)) {
				globalWasProcessed = true;
				lknames[size++] = lp.name;
			}
		}

		lknames2 = new String[size];
		for (int i = 0; i < size; i++) {
			lknames2[i] = lknames[i];
		}

		return lknames2;
	}

	/**
	 * Francesco D'Aquino
	 * Normalizes the routing probabilities for each station
	 */
	@Override
	public void manageProbabilities() {
		//get the vector of the stations
		Vector<Object> stations = getStationKeys();
		//get the vector of the classes
		Vector<Object> classes = getClassKeys();
		//for each station ...
		for (int i = 0; i < stations.size(); i++) {
			//get the station at i from the station vector
			Object thisStation = stations.get(i);
			//if it is not a sink...
			if (!getStationType(thisStation).equals(CommonConstants.STATION_TYPE_SINK)) {
				//Aboce :-^All stations have router except Sink
				//for each class...
				for (int j = 0; j < classes.size(); j++) {
					Object thisClass = classes.get(j);
					//check if the routing strategy in thisStation is ProbabilityRouting
					if (getRoutingStrategy(thisStation, thisClass) instanceof ProbabilityRouting) {
						//if it is so, normalize routing probabilities
						Vector<Object> outputKeys = getForwardConnections(thisStation);
						ProbabilityRouting pr = (ProbabilityRouting) getRoutingStrategy(thisStation, thisClass);
						Map<Object, Double> values = pr.getValues();
						normalizeProbabilities(values, outputKeys, thisClass, thisStation);
					}
				}
			}
		}
	}

	// NB: cut from jmt.gui.common.xml.XMLWriter
	public void normalizeProbabilities(Map<Object,Double> values, Vector<Object> outputKeys, Object thisClassKey, Object thisStation) {
		Vector<Object> sinkClosedprobabilities = new Vector<Object>();
		Vector<Object> normalProbabilities = new Vector<Object>();
		boolean allSink = true;
		for (int i=0; i<outputKeys.size(); i++) {
			if (isClosedClassSinkProbability(outputKeys.get(i),thisClassKey)) {
				sinkClosedprobabilities.add(outputKeys.get(i));
			} else {
				normalProbabilities.add(outputKeys.get(i));
				allSink = false;
			}			
		}
		if (allSink) {
			normalProbabilities.addAll(sinkClosedprobabilities);
			sinkClosedprobabilities.clear();
		}
		Double[] probabilities = new Double[outputKeys.size()];
		Object[] keys = new Object[outputKeys.size()];
		outputKeys.toArray(keys);
		//extract all values from map in array form
		for (int i=0; i<keys.length; i++) {
			probabilities[i] = (Double)values.get(keys[i]);
		}
		for (int i=0; i<probabilities.length; i++) {	    	
			if (probabilities[i] != null && probabilities[i].doubleValue() != 0.0 && sinkClosedprobabilities.contains(keys[i])) {
				probabilities[i] = new Double(0.0);
				String className = getClassName(thisClassKey);
				String stationName = getStationName(thisStation);
				if (!sinkProbabilityUpdateClasses.contains(className)) {//I dont want the Names repeated.
					sinkProbabilityUpdateClasses.add(className);
				}
				if (!sinkProbabilityUpdateStations.contains(stationName)) {
					sinkProbabilityUpdateStations.add(stationName);
				}	        	
				sinkProbabilityUpdate = true;
			}	    	
		}
		values.clear();
		//scan for null values and for total sum
		double totalSum = 0.0;
		int totalNonNull = 0;
		boolean allNull = true;
		for (int i=0; i<probabilities.length; i++) {
			if (probabilities[i]!=null && normalProbabilities.contains(keys[i])) {
				totalSum += probabilities[i].doubleValue();
				totalNonNull++;
				allNull = false;
			}
		}
		//modify non null values for their sum to match 1 and put null values to 1
		for (int i=0; i<probabilities.length; i++) {
			if ((probabilities[i]!=null || allNull) && normalProbabilities.contains(keys[i])) {
				if (totalSum==0) {
					probabilities[i] = new Double(1.0/totalNonNull);
				} else {
					probabilities[i] = new Double(probabilities[i].doubleValue()/totalSum);
				}
			} else {
				probabilities[i] = new Double(0.0);
			}
			values.put(keys[i], probabilities[i]);
		}
	}

	// M Cazzoli
	public void normalizeProbabilitiesFork(Map<Object, Double> values) {
		Set<Object> keys = values.keySet();

		Double[] probabilities = new Double[keys.size()];
		Object[] keyList = keys.toArray();
		// extract all values from map in array form
		for (int i = 0; i < keyList.length; i++) {
			probabilities[i] = (Double) values.get(keyList[i]);
		}

		values.clear();
		// scan for null values and for total sum
		double totalSum = 0.0;
		int totalNonNull = 0;
		boolean allNull = true;
		for (int i = 0; i < probabilities.length; i++) {
			if (probabilities[i] != null) {
				totalSum += probabilities[i].doubleValue();
				totalNonNull++;
				allNull = false;
			}
		}
		// modify non null values for their sum to match 1 and put null values
		// to 1
		for (int i = 0; i < probabilities.length; i++) {
			if ((probabilities[i] != null || allNull)) {
				if (totalSum == 0) {
					probabilities[i] = new Double(1.0 / totalNonNull);
				} else {
					probabilities[i] = new Double(
							probabilities[i].doubleValue() / totalSum);
				}
			} else {
				probabilities[i] = new Double(0.0);
			}
			values.put(keyList[i], probabilities[i]);
		}
	}

	private boolean isClosedClassSinkProbability(Object stationKey, Object classKey) {
		boolean ret = false;
		if (this.getStationType(stationKey).equals(CommonConstants.STATION_TYPE_SINK)
				&& this.getClassType(classKey) == CommonConstants.CLASS_TYPE_CLOSED) {
			ret = true;
		}
		return ret;
	}

	/**
	 * Used to set Defaults Class-Station parameters for a new Station.
	 * This method is called when a station type is changed too.
	 * @param sd StationData data structure for current station
	 * @return created StationClassData object
	 * Modified by Bertoli Marco
	 */
	private StationClassData getDefaultDetailsForStationType(StationData sd) {
		StationClassData defaultDetails = null;
		if (sd == null) {
			return null;
		}

		String queueStrategy = QUEUE_STRATEGY_FCFS;
		if (STATION_TYPE_SERVER.equals(sd.type)) {
			queueStrategy = Defaults.get("stationQueueStrategy");
		} else if (STATION_TYPE_FORK.equals(sd.type)) {
			queueStrategy = Defaults.get("stationQueueStrategy");
			if (queueStrategy.equals(QUEUE_STRATEGY_SJF) || queueStrategy.equals(QUEUE_STRATEGY_LJF)) {
				queueStrategy = QUEUE_STRATEGY_FCFS;
			}
		}

		if (STATION_TYPE_SOURCE.equals(sd.type)) {
			defaultDetails = new StationClassData(null, null,
					Defaults.getAsNewInstance("stationRoutingStrategy"), null, null, null, null, null, null, null, null, null, null);
		} else if (STATION_TYPE_SINK.equals(sd.type)) {
			defaultDetails = new StationClassData(null, null, null, null, null, null, null, null, null, null, null, null, null);
		} else if (STATION_TYPE_TERMINAL.equals(sd.type)) {
			defaultDetails = new StationClassData(null, null,
					Defaults.getAsNewInstance("stationRoutingStrategy"), null, null, null, null, null, null, null, null, null, null);
		} else if (STATION_TYPE_ROUTER.equals(sd.type)) {
			defaultDetails = new StationClassData(
					queueStrategy, null,
					Defaults.getAsNewInstance("stationRoutingStrategy"), null,
					Defaults.get("dropRule"), null, null, null, null, null, null, null, null);
		} else if (STATION_TYPE_DELAY.equals(sd.type)) {
			defaultDetails = new StationClassData(
					queueStrategy,
					Defaults.getAsNewInstance("stationDelayServiceStrategy"),
					Defaults.getAsNewInstance("stationRoutingStrategy"), null,
					Defaults.get("dropRule"), null, null, null, null, null, null, null, null);
		} else if (STATION_TYPE_SERVER.equals(sd.type)) {
			defaultDetails = new StationClassData(
					queueStrategy,
					Defaults.getAsNewInstance("stationServiceStrategy"),
					Defaults.getAsNewInstance("stationRoutingStrategy"), null,
					Defaults.get("dropRule"), null, null, null, null, null, null, null, null);
		} else if (STATION_TYPE_FORK.equals(sd.type)) {
			defaultDetails = new StationClassData(
					queueStrategy, null, null, null,
					Defaults.get("dropRule"),
					Defaults.getAsNewInstance("stationForkingStrategy"), null, null, null, null, null, null, null);
		} else if (STATION_TYPE_JOIN.equals(sd.type)) {
			defaultDetails = new StationClassData(null, null,
					Defaults.getAsNewInstance("stationRoutingStrategy"), null, null, null,
					Defaults.getAsNewInstance("stationJoinStrategy"), null, null, null, null, null, null);
		} else if (STATION_TYPE_LOGGER.equals(sd.type)) {
			defaultDetails = new StationClassData(
					queueStrategy, null,
					Defaults.getAsNewInstance("stationRoutingStrategy"), null,
					Defaults.get("dropRule"), null, null, null, null, null, null, null, null);
		} else if (STATION_TYPE_CLASSSWITCH.equals(sd.type)) {
			defaultDetails = new StationClassData(
					queueStrategy, null,
					Defaults.getAsNewInstance("stationRoutingStrategy"),
					Defaults.getAsNewInstance("stationCsRowMatrix"),
					Defaults.get("dropRule"), null, null, null, null, null, null, null, null);
		} else if (STATION_TYPE_SEMAPHORE.equals(sd.type)) {
			defaultDetails = new StationClassData(null, null,
					Defaults.getAsNewInstance("stationRoutingStrategy"), null, null, null, null,
					Defaults.getAsNewInstance("stationSemaphoreStrategy"), null, null, null, null, null);
		} else if (STATION_TYPE_SCALER.equals(sd.type)) {
			defaultDetails = new StationClassData(null, null, null, null, null,
					Defaults.getAsNewInstance("stationForkingStrategy"),
					Defaults.getAsNewInstance("stationJoinStrategy"), null, null, null, null, null, null);
		} else if (STATION_TYPE_PLACE.equals(sd.type)) {
			defaultDetails = new StationClassData(
					Defaults.get("storeQueueStrategy"), null, null, null,
					Defaults.get("storeDropRule"), null, null, null, null, null, null, null, null);
		} else if (STATION_TYPE_TRANSITION.equals(sd.type)) {
			defaultDetails = new StationClassData(null,
					Defaults.getAsNewInstance("transitionTimingStrategy"), null, null, null, null, null, null,
					Defaults.getAsNewInstance("transitionVector"),
					Defaults.getAsNewInstance("transitionVector"),
					Defaults.getAsNewInstance("transitionVector"),
					Defaults.getAsInteger("transitionFiringPriority"),
					Defaults.getAsInteger("transitionFiringWeight"));
		}

		return defaultDetails;
	}

	/**Adds details for a new station. For each type of station proper parameter
	 * values are set.*/
	protected void addNewStationDetails(Object stationKey) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		Vector<Object> classKeys = getClassKeys();
		for (int i = 0; i < classKeys.size(); i++) {
			stationDetailsBDM.put(classKeys.get(i), stationKey, getDefaultDetailsForStationType(sd));
		}
	}

	/**Adds details for a new station. For each type of station proper parameter
	 * values are set.*/
	protected void addStationDetailsForClass(Object classKey) {
		Vector<Object> stationKeys = getStationKeys();
		for (int i = 0; i < stationKeys.size(); i++) {
			StationData sd = (StationData) stationDataHM.get(stationKeys.get(i));
			StationClassData defaultDetails = getDefaultDetailsForStationType(sd);
			stationDetailsBDM.put(classKey, stationKeys.get(i), defaultDetails);
		}
	}

	/**Deletes station details from repository, given station search key.*/
	protected void deleteStationDetails(Object stationKey) {
		stationDetailsBDM.remove(stationKey, BDMap.Y);
	}

	/**Deletes station details about a certain class, given its search key*/
	protected void deleteStationDetailsForClass(Object classKey) {
		stationDetailsBDM.remove(classKey, BDMap.X);
		for (Object station: this.getStationKeys()) {
			for (Object classK: this.getClassKeys()) {
				Object strategy = this.getForkingStrategy(station, classK);
				if (strategy instanceof MultiBranchClassSwitchFork) {
					Map<Object, OutPath> m = ((MultiBranchClassSwitchFork) strategy).getOutDetails();
					for (OutPath o: m.values()) {
						o.getOutParameters().remove(classKey);
					}
				}
				if (strategy instanceof ClassSwitchFork) {
					Map<Object, OutPath> m = ((ClassSwitchFork) strategy).getOutDetails();
					for (OutPath o: m.values()) {
						o.getOutParameters().remove(classKey);
					}
				}
				strategy = this.getJoinStrategy(station, classK);
				if (strategy instanceof GuardJoin) {
					((GuardJoin) strategy).getGuard().remove(classKey);
				}
				/*strategy = this.getSemaphoreStrategy(station, classK);
				if (strategy instanceof NormalSemaphore) {
					((NormalSemaphore) strategy).getSemaphore().remove(classKey);
				}*/
			}
		}
	}

	/*------------------------------------------------------------------------------
	 *-------------  methods for inter-station connections definition  --------------
	 *-------------------------------------------------------------------------------*/
	/**Adds a connection between two stations in this model, given search keys of
	 * source and target stations. If connection could not be created (if, for example,
	 * target station's type is "Source") false value is returned.
	 * @param sourceKey: search key for source station
	 * @param targetKey: search key for target station
	 * @param areConnected: true if stations must be connected, false otherwise.
	 * @return : true if connection was created, false otherwise.
	 * */
	@Override
	public boolean setConnected(Object sourceKey, Object targetKey, boolean areConnected) {
		Connection toSet = (Connection) connectionsBDM.get(targetKey, sourceKey);
		//element does not exist, no further operation possible, return.
		if (toSet == null) {
			return false;
		}
		//exists but is not connectable, return.
		if (!toSet.isConnectable) {
			return false;
		}
		//is already been connected: cannot be connected twice, but can be disconnected
		if (toSet.isConnected) {
			save = true;
			//must disconnect
			if (!areConnected) {
				toSet.isConnected = false;
			}
		} else {
			toSet.isConnected = areConnected;
			if (areConnected) {
				for (Object c: this.getClassKeys()) {
					ForkStrategy fs = (ForkStrategy) this.getForkingStrategy(sourceKey, c);
					if (fs != null) {
						fs.addStation(this.getStationName(targetKey), targetKey, c, this.getClassKeys());
					}
				}                    
			}
			return toSet.isConnected;
		}
		return false;
	}

	/**Tells whether two stations are connected
	 * @param sourceKey: search key for source station
	 * @param targetKey: search key for target station
	 * @return : true if stations are connected, false otherwise.
	 */
	@Override
	public boolean areConnected(Object sourceKey, Object targetKey) {
		Connection conn = (Connection) connectionsBDM.get(targetKey, sourceKey);
		if (conn == null) {
			return false;
		} else {
			return conn.isConnected;
		}
	}

	/**Tells whether two stations can be connected
	 * @param sourceKey: search key for source station
	 * @param targetKey: search key for target station
	 * @return : true if stations are connectable, false otherwise.
	 */
	@Override
	public boolean areConnectable(Object sourceKey, Object targetKey) {
		Connection conn = (Connection) connectionsBDM.get(targetKey, sourceKey);
		if (conn == null) {
			return false;
		} else {
			return conn.isConnectable;
		}
	}

	/**Returns a set of station keys specified station is connected to as a source.
	 * @param stationKey: source station for which (target)connected stations must be
	 * returned.
	 * @return Vector containing keys for connected stations.
	 */
	@Override
	public Vector<Object> getForwardConnections(Object stationKey) {
		//must find entry for row index (e.g. connection sources set)
		Map conns = connectionsBDM.get(stationKey, BDMap.Y);
		return scanForConnections(conns);
	}

	/**Returns a set of station keys specified station is connected to as a target.
	 * @param stationKey: source station for which (source)connected stations must be
	 * returned.
	 * @return Vector containing keys for connected stations.
	 */
	@Override
	public Vector<Object> getBackwardConnections(Object stationKey) {
		//must find entry for row index (e.g. connection targets set)
		Map conns = connectionsBDM.get(stationKey, BDMap.X);
		return scanForConnections(conns);
	}

	//finds out connections, given a connection set
	private Vector<Object> scanForConnections(Map conns) {
		Vector<Object> retval = new Vector<Object>(0);
		//check returned map not to be null
		if (conns != null) {
			//scan all of the station entries to find out which selected one is connected to
			for (int i = 0; i < stationsKeyset.size(); i++) {
				Connection c = (Connection) conns.get(stationsKeyset.get(i));
				//assure connection is not null, preventing NullPointerException
				if (c != null) {
					//finally, if connection exists and is connected, add key to returned vector
					if (c.isConnected) {
						retval.add(stationsKeyset.get(i));
					}
				}
			}
		}
		return retval;
	}

	/**Tells whether two stations can be connected. Subclasses can override this method to
	 * change behaviour in connection creation.*/
	protected boolean canBeConnected(Object sourceKey, Object targetKey) {
		StationData source = (StationData) stationDataHM.get(sourceKey), target = (StationData) stationDataHM.get(targetKey);
		/*if source and target are both servers or delays or LDServers, at first
		instance, declare them connectable*/
		if ((source.type.equals(STATION_TYPE_SERVER) || source.type.equals(STATION_TYPE_DELAY))
				&& (target.type.equals(STATION_TYPE_SERVER) || target.type.equals(STATION_TYPE_DELAY))) {
			return true;
		}
		//no incoming connection to source
		if (target.type.equals(STATION_TYPE_SOURCE)) {
			return false;
		}
		//no outgoing connections from sink
		if (source.type.equals(STATION_TYPE_SINK)) {
			return false;
		}
		// No connection between a router, fork, join, logger, semaphore or scaler and itself
		if (sourceKey == targetKey
				&& (source.type.equals(STATION_TYPE_ROUTER) || source.type.equals(STATION_TYPE_FORK) || source.type.equals(STATION_TYPE_JOIN)
						|| source.type.equals(STATION_TYPE_LOGGER) || source.type.equals(STATION_TYPE_SEMAPHORE) || source.type.equals(STATION_TYPE_SCALER))) {
			return false;
		}
		//no direct connections from source to sink
		//no loops on terminal
		//no direct connections between terminal and source or sink
		if ((source.type.equals(STATION_TYPE_SOURCE) || source.type.equals(STATION_TYPE_SINK) || source.type.equals(STATION_TYPE_TERMINAL))
				&& (target.type.equals(STATION_TYPE_SOURCE) || target.type.equals(STATION_TYPE_SINK) || target.type.equals(STATION_TYPE_TERMINAL))) {
			return false;
		}
		// A place can only be connected to a transition.
		// A transition can only be connected from a place.
		if ((source.type.equals(STATION_TYPE_PLACE) && !target.type.equals(STATION_TYPE_TRANSITION))
				|| (target.type.equals(STATION_TYPE_TRANSITION) && !source.type.equals(STATION_TYPE_PLACE))) {
			return false;
		}
		return true;
	}

	/*Creates all of the entries inside connection data repository for a new station.*/
	private void addNewStationConnections(Object stationKey) {
		//first add all of the forward connections
		//picking all possible targets
		Vector<Object> keys = new Vector<Object>(connectionsBDM.keySet(BDMap.X));
		int size = keys.size();
		Map<Object, Connection> conns = new HashMap<Object, Connection>();
		//building set of forward connections
		for (int i = 0; i < size; i++) {
			//current station key
			Object current = keys.get(i);
			conns.put(current, new Connection(canBeConnected(stationKey, current), false));
		}
		connectionsBDM.put(stationKey, BDMap.Y, conns);
		//reset all data structures for bw connections addition
		//picking all possible sources
		keys = new Vector<Object>(connectionsBDM.keySet(BDMap.Y));
		size = keys.size();
		conns = new HashMap<Object, Connection>();
		//building set of backward connections
		for (int i = 0; i < size; i++) {
			//current station key
			Object current = keys.get(i);
			conns.put(current, new Connection(canBeConnected(current, stationKey), false));
		}
		connectionsBDM.put(stationKey, BDMap.X, conns);
	}

	//deletes connections for deleted station
	private void deleteStationConnections(Object stationKey) {
		connectionsBDM.remove(stationKey, BDMap.X);
		connectionsBDM.remove(stationKey, BDMap.Y);
	}

	/*------------------------------------------------------------------------------
	 *---------------  methods for simulation parameters definition  ----------------
	 *-------------------------------------------------------------------------------*/
	/* (non-Javadoc)
	 * @see jmt.gui.common.definitions.SimulationDefinition#addMeasure(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public Object addMeasure(String type, Object stationKey, Object classKey) {
		return addMeasure(type, stationKey, classKey, Defaults.getAsDouble("measureAlpha"), Defaults.getAsDouble("measurePrecision"), false);
	}

	/* (non-Javadoc)
	 * @see jmt.gui.common.definitions.SimulationDefinition#addMeasure(java.lang.String, java.lang.Object, java.lang.Object, java.lang.Double, java.lang.Double, jmt.gui.common.definitions.OutputParameters)
	 */
	@Override
	public Object addMeasure(String type, Object stationKey, Object classKey, Double alpha, Double precision, boolean log) {
		Object key = new Long(++incrementalKey);
		MeasureData md = new MeasureData(stationKey, classKey, type, alpha, precision, log);
		measureDataHM.put(key, md);
		measuresKeyset.add(key);
		save = true;
		return key;
	}

	@Override
	public void removeMeasure(Object measureKey) {
		if (measuresKeyset.contains(measureKey)) {
			measuresKeyset.remove(measureKey);
			measureDataHM.remove(measureKey);
			save = true;
		}
	}

	private void deleteStationMeasures(Object key) {
		for (Object measureKey: measuresKeyset.toArray()) {
			if (getMeasureStation(measureKey) == key) {
				removeMeasure(measureKey);
			}
		}
	}

	@Override
	public Vector<Object> getMeasureKeys() {
		return measuresKeyset;
	}

	@Override
	public String getMeasureType(Object measureKey) {
		if (!measuresKeyset.contains(measureKey)) {
			return null;
		} else {
			return measureDataHM.get(measureKey).type;
		}
	}

	@Override
	public void setMeasureType(String newType, Object measureKey) {
		if (measuresKeyset.contains(measureKey)) {
			String oldType = measureDataHM.get(measureKey).type;
			measureDataHM.get(measureKey).type = newType;
			if (!oldType.equals(newType)) {
				save = true;
			}
		}
	}

	@Override
	public Object getMeasureClass(Object measureKey) {
		if (!measuresKeyset.contains(measureKey)) {
			return null;
		} else {
			return measureDataHM.get(measureKey).classKey;
		}
	}

	@Override
	public void setMeasureClass(Object classKey, Object measureKey) {
		if (measuresKeyset.contains(measureKey)) {
			Object oldKey = measureDataHM.get(measureKey).classKey;
			measureDataHM.get(measureKey).classKey = classKey;
			if (oldKey != classKey) {
				save = true;
			}
		}
	}

	@Override
	public Object getMeasureStation(Object measureKey) {
		if (!measuresKeyset.contains(measureKey)) {
			return null;
		} else {
			return measureDataHM.get(measureKey).stationKey;
		}
	}

	@Override
	public void setMeasureStation(Object stationKey, Object measureKey) {
		if (measuresKeyset.contains(measureKey)) {
			Object oldKey = measureDataHM.get(measureKey).stationKey;
			measureDataHM.get(measureKey).stationKey = stationKey;
			if (oldKey != stationKey) {
				save = true;
			}
		}
	}

	@Override
	public Double getMeasureAlpha(Object measureKey) {
		if (!measuresKeyset.contains(measureKey)) {
			return null;
		} else {
			return measureDataHM.get(measureKey).alpha;
		}
	}

	@Override
	public void setMeasureAlpha(Double alpha, Object measureKey) {
		if (alpha.doubleValue() > 0 && alpha.doubleValue() < 1) {
			if (measuresKeyset.contains(measureKey)) {
				Double oldAlpha = measureDataHM.get(measureKey).alpha;
				measureDataHM.get(measureKey).alpha = alpha;
				if (!oldAlpha.equals(alpha)) {
					save = true;
				}
			}
		}
	}

	@Override
	public Double getMeasurePrecision(Object measureKey) {
		if (!measuresKeyset.contains(measureKey)) {
			return null;
		} else {
			return measureDataHM.get(measureKey).precision;
		}
	}

	@Override
	public void setMeasurePrecision(Double precision, Object measureKey) {
		if (precision.doubleValue() > 0 && precision.doubleValue() < 1) {
			if (measuresKeyset.contains(measureKey)) {
				Double oldPrecision = measureDataHM.get(measureKey).precision;
				measureDataHM.get(measureKey).precision = precision;
				if (!oldPrecision.equals(precision)) {
					save = true;
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see jmt.gui.common.definitions.SimulationDefinition#setLogMeasure(boolean, java.lang.Object)
	 */
	@Override
	public void setMeasureLog(boolean log, Object measureKey) {
		MeasureData data = measureDataHM.get(measureKey);
		if (data != null && data.log != log) {
			data.log = log;
			save = true;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.gui.common.definitions.SimulationDefinition#getLogMeasure(java.lang.Object)
	 */
	@Override
	public boolean getMeasureLog(Object measureKey) {
		if (!measuresKeyset.contains(measureKey)) {
			return false;
		} else {
			return measureDataHM.get(measureKey).log;
		}
	}

	/**
	 * Tells if a given measure is global or local (i.e. if it is station independent
	 * or station dependent)
	 * <br>Author: Bertoli Marco
	 * @param key search's key for given measure
	 * @return true if measure is global
	 */
	@Override
	public boolean isGlobalMeasure(Object key) {
		String type = getMeasureType(key);
		return type.equals(SimulationDefinition.MEASURE_S_X) || type.equals(SimulationDefinition.MEASURE_S_RP)
				|| type.equals(SimulationDefinition.MEASURE_S_CN) || type.equals(SimulationDefinition.MEASURE_S_DR)
				//Added by ASHANKA START
				//Here this helps to avoid the display of the Individual Queue Service Stations in the 
				//Combo Box in the Measure Panel as System Power is calculated at whole System or each class
				//level but has no meaning for each station level.
				|| type.equals(SimulationDefinition.MEASURE_S_SP)
				//Added by ASHANKA END
				;
	}

	/**
	 * Tells if the Measure is being calculated keeping the Sink as reference,
	 * Is used for the performance indices of
	 * ResponseTimePerSink and ThroughputPerSink.
	 * @param key
	 * @return
	 */
	@Override
	public boolean isSinkMeasure(Object key) {
		String type = getMeasureType(key);
		boolean ret = false;
		ret = (type.equals(SimulationDefinition.MEASURE_R_PER_SINK) 
				|| type.equals(SimulationDefinition.MEASURE_X_PER_SINK)) ? true : false;
		return ret;
	}

	@Override
	public boolean isFCRMeasure(Object key) {
		String type = getMeasureType(key);
		boolean ret = false;
		ret = (type.equals(SimulationDefinition.MEASURE_FCR_TOTAL_WEIGHT)
				|| type.equals(SimulationDefinition.MEASURE_FCR_MEMORY_OCCUPATION)) ? true : false;
		return ret;
	}

	@Override
	public boolean isFJMeasure(Object key) {
		String type = getMeasureType(key);
		boolean ret = false;
		ret = (type.equals(SimulationDefinition.MEASURE_FJ_CN)
				|| type.equals(SimulationDefinition.MEASURE_FJ_RESPONSE_TIME)) ? true : false;
		return ret;
	}

	/**
	 * Sets preloaded jobs on a given station for a given class
	 * @param jobs number of jobs to be put in queue
	 * @param stationKey search's key for station
	 * @param classKey search's key for class
	 * Author: Bertoli Marco
	 */
	@Override
	public void setPreloadedJobs(Integer jobs, Object stationKey, Object classKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null) {
			if (!current.preload.equals(jobs)) {
				save = true;
			}
			current.preload = jobs;
		}
	}

	/**
	 * Gets preloaded jobs on a given station for given class
	 * @param stationKey search's key for station
	 * @param classKey search's key for class
	 * @return preloaded jobs
	 * Author: Bertoli Marco
	 */
	@Override
	public Integer getPreloadedJobs(Object stationKey, Object classKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null) {
			return current.preload;
		} else {
			return null;
		}
	}

	/**
	 * Returns number of jobs totally allocated for specified class.
	 * @param classKey class jobs to be preloaded belong to.
	 * @return number of jobs totally to be preloaded.
	 */
	@Override
	public synchronized Integer getPreloadedJobsNumber(Object classKey) {
		int foundJob = 0;
		for (int i = 0; i < stationsKeyset.size(); i++) {
			foundJob += this.getPreloadedJobs(stationsKeyset.get(i), classKey).intValue();
		}
		return new Integer(foundJob);
	}

	@Override
	public boolean getUseRandomSeed() {
		return useRandomSeed;
	}

	@Override
	public void setUseRandomSeed(boolean value) {
		if (useRandomSeed != value) {
			save = true;
		}
		this.useRandomSeed = value;
	}

	@Override
	public void setSimulationSeed(Long seed) {
		if (!this.seed.equals(seed)) {
			save = true;
		}
		this.seed = seed;
	}

	@Override
	public Long getSimulationSeed() {
		return seed;
	}

	@Override
	public void setMaximumDuration(Double durationSeconds) {
		if (!this.maxDuration.equals(durationSeconds)) {
			save = true;
		}
		this.maxDuration = durationSeconds;
	}

	@Override
	public Double getMaximumDuration() {
		return maxDuration;
	}

	@Override
	public void setMaxSimulatedTime(Double duration)
	{
		if (!this.maxSimulatedTime.equals(duration)) {
			save = true;
		}
		this.maxSimulatedTime = duration;
	}

	@Override
	public Double getMaxSimulatedTime()
	{
		return maxSimulatedTime;
	}

	/**
	 * Gets polling interval for temporary measures
	 * @return polling interval for temporary measures
	 */
	@Override
	public double getPollingInterval() {
		return pollingInterval;
	}

	/**
	 * Sets polling interval for temporary measures
	 * @param pollingInterval polling interval for temporary measures
	 */
	@Override
	public void setPollingInterval(double pollingInterval) {
		if (this.pollingInterval != pollingInterval) {
			save = true;
		}
		this.pollingInterval = pollingInterval;
	}

	/**
	 * Sets maximum number of simulation samples
	 *
	 * @param maxSamples maximum number of simulation samples
	 */
	@Override
	public void setMaxSimulationSamples(Integer maxSamples) {
		if (!this.maxSamples.equals(maxSamples)) {
			save = true;
		}
		this.maxSamples = maxSamples;
	}

	/**
	 * Returns maximum number of simulation samples
	 *
	 * @return maximum number of simulation samples
	 */
	@Override
	public Integer getMaxSimulationSamples() {
		return maxSamples;
	}

	/**
	 * Tells if statistic check was disabled as simulation stopping criteria
	 * @return the disableStatistic
	 */
	@Override
	public Boolean getDisableStatistic() {
		return disableStatistic;
	}

	/**
	 * Sets if statistic check was disabled as simulation stopping criteria
	 * @param disableStatistic the disableStatistic to set
	 */
	@Override
	public void setDisableStatistic(Boolean disableStatistic) {
		this.disableStatistic = disableStatistic;
	}

	/**
	 * This method is used to manage number of jobs for a given class. If class is closed
	 * all spare jobs will be allocated to its reference source (if it is not in a
	 * blocking region, otherwise a different station is chosen), if for some reasons more
	 * jobs are allocated than max population, they are reduced.
	 * @param classKey search's key for class to be considered
	 * Author: Bertoli Marco
	 */
	public synchronized void manageJobs(Object classKey) {
		if (this.getClassType(classKey) == CLASS_TYPE_CLOSED) {
			int population = this.getClassPopulation(classKey).intValue();
			int foundJob = getPreloadedJobsNumber(classKey).intValue();
			if (population > foundJob) {
				Object chosenStation = this.getClassRefStation(classKey);
				if (chosenStation == null) {
					return;
				}
				// If refStation is not preloadable or is inside a blocking region, choose the preloadable station
				// that is not inside any blocking region and has the most preloaded jobs
				if (!isStationTypePreloadable(getStationType(chosenStation)) || getStationBlockingRegion(chosenStation) != null) {
					int preloadMax = -1;
					for (int i = 0; i < stationsKeyset.size(); i++) {
						Object key = stationsKeyset.get(i);
						if (isStationTypePreloadable(getStationType(key)) && getStationBlockingRegion(key) == null
								&& getPreloadedJobs(key, classKey).intValue() > preloadMax) {
							preloadMax = getPreloadedJobs(key, classKey).intValue();
							chosenStation = key;
						}
					}
				}
				// Increments amount of jobs in chosen station to cover all spare one
				int allocate = population - foundJob + this.getPreloadedJobs(chosenStation, classKey).intValue();
				this.setPreloadedJobs(new Integer(allocate), chosenStation, classKey);
			} else if (population < foundJob) {
				// Removes jobs from stations until allocated jobs = population
				for (int i = 0; i < stationsKeyset.size() && population < foundJob; i++) {
					int jobs = this.getPreloadedJobs(stationsKeyset.get(i), classKey).intValue();
					if (jobs <= foundJob - population) {
						this.setPreloadedJobs(new Integer(0), stationsKeyset.get(i), classKey);
						foundJob -= jobs;
					} else {
						this.setPreloadedJobs(new Integer(jobs - (foundJob - population)), stationsKeyset.get(i), classKey);
						break;
					}
				}
			}
		}
	}

	/**
	 * This method is used to manage number of jobs for every class. If class is closed
	 * all spare jobs will be allocated to its reference source, if for some reasons more
	 * jobs are allocated than max population, they are reduced. Uses this method only
	 * when strictly necessary as is can be slow if the model is big.
	 * Author: Bertoli Marco
	 */
	@Override
	public void manageJobs() {
		for (int i = 0; i < classesKeyset.size(); i++) {
			manageJobs(classesKeyset.get(i));
		}
	}

	// --- Methods to manage simulation results -- Bertoli Marco --------------------------------------------
	/**
	 * Returns last simulation results
	 * @return simulation results or null if no simulation was performed
	 */
	@Override
	public MeasureDefinition getSimulationResults() {
		return results;
	}

	/**
	 * Sets simulation results
	 * @param results simulation results data structure
	 */
	@Override
	public void setSimulationResults(MeasureDefinition results) {
		this.results = results;
		save = true;
	}

	/**
	 * Tells if current model contains simulation results
	 * @return true iff <code>getSimulationResults()</code> returns a non-null object
	 */
	@Override
	public boolean containsSimulationResults() {
		return (results != null);
	}

	//Francesco D'Aquino ----------------------

	/**
	 * Return true if queue animation is enabled
	 *
	 * @return true if the animation is enabled
	 */
	@Override
	public boolean isAnimationEnabled() {
		return false;
	}

	/**
	 * Enable or disable queue animation
	 *
	 * @param isEnabled - set it to true to enable queue animation
	 */
	@Override
	public void setAnimationEnabled(boolean isEnabled) {

	}

	/**
	 * Checks if the parametric analysis has been enabled
	 *
	 * @return true if the parametric analysis has been enabled
	 */
	@Override
	public boolean isParametricAnalysisEnabled() {
		return parametricAnalysisEnabled;
	}

	/**
	 * Enable / disable parametric analysis
	 * @param enabled
	 */
	@Override
	public void setParametricAnalysisEnabled(boolean enabled) {
		if (parametricAnalysisEnabled != enabled) {
			save = true;
		}
		parametricAnalysisEnabled = enabled;
	}

	/**
	 * Sets the parametric analysis definition
	 *
	 * @param pad the parametric analysis definition to be set
	 */
	@Override
	public void setParametricAnalysisModel(ParametricAnalysisDefinition pad) {
		parametricAnalysisModel = pad;
	}

	/**
	 * Tells model that some data has been changed and need to be saved. This
	 * is used by Parametric Analysis
	 */
	@Override
	public void setSaveChanged() {
		save = true;
	}

	/**
	 * Gets the ParametricAnalysisModel. If parametric analysis is not enabled
	 * the returned value may be not meaningful.
	 * @return the parametricAnalysisModel
	 */
	@Override
	public ParametricAnalysisDefinition getParametricAnalysisModel() {
		return parametricAnalysisModel;
	}

	/**
	 * Returns the class key given its name. It returns <code>null</code>
	 * if no such class is found.
	 * @param className the name of the class
	 * @return  the key of the class whose name is <code>className</code>
	 */
	@Override
	public Object getClassByName(String className) {
		for (int i = 0; i < classesKeyset.size(); i++) {
			Object thisClass = classesKeyset.get(i);
			if (getClassName(thisClass).equals(className)) {
				return thisClass;
			}
		}
		return null;
	}

	/**
	 * Returns the key of the station whose name is <code>stationName</code>. It
	 * returns <code>null</code> if no such station is found.
	 * @param stationName the name of the station
	 * @return the key of the station
	 */
	@Override
	public Object getStationByName(String stationName) {
		for (int i = 0; i < stationsKeyset.size(); i++) {
			Object thisStation = stationsKeyset.get(i);
			if (getStationName(thisStation).equals(stationName)) {
				return thisStation;
			}
		}
		return null;
	}

	// end Francesco D'Aquino ------------------------------

	// --- Blocking Region Definition --- Bertoli Marco ----------------------------------------------
	/**
	 * Adds a new blocking region to the model
	 *
	 * @param name name of new blocking region
	 * @param type type of new blocking region
	 * @return search's key for new blocking region
	 */
	@Override
	public Object addBlockingRegion(String name, String type) {
		Object key = new Long(++incrementalKey);
		//If this blocking region has already been created, do not add search key to the list.
		if (!blockingRegionsKeyset.contains(key)) {
			blockingRegionsKeyset.add(key);
		}
		BlockingRegionData newBlocking = new BlockingRegionData(name, type, Defaults.getAsInteger("blockingMaxJobs"), Defaults.getAsInteger("blockingMaxMemory"));
		blockingDataHM.put(key, newBlocking);
		// Adds region-class specific data
		addBlockingDetails(key);
		save = true;
		return key;
	}

	/**
	 * Removes a blocking region from the model
	 *
	 * @param key Search's key for region to be deleted
	 */
	@Override
	public void deleteBlockingRegion(Object key) {
		if (blockingRegionsKeyset.contains(key)) {
			BlockingRegionData bd = (BlockingRegionData) blockingDataHM.remove(key);
			// Removes blocking region from station reference
			Iterator<Object> it = bd.stations.iterator();
			while (it.hasNext()) {
				Object stationKey = it.next();
				((StationData) stationDataHM.get(stationKey)).blockingRegion = null;
				// Adds this station to blockable ones
				if (canStationTypeBeBlocked(((StationData) stationDataHM.get(stationKey)).type)) {
					blockableStations.add(stationKey);
				}
			}
			blockingRegionsKeyset.remove(key);

			deleteBlockingDetails(key);
			save = true;
		}
	}

	/**
	 * Adds a new station to specified blocking region
	 *
	 * @param regionKey  Search's key for region
	 * @param stationKey Search's key for station
	 * @return true if station can be put inside specified region
	 */
	@Override
	public boolean addRegionStation(Object regionKey, Object stationKey) {
		if (!this.canRegionStationBeAdded(regionKey, stationKey)) {
			return false;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).stations.add(stationKey);
		((StationData) stationDataHM.get(stationKey)).blockingRegion = regionKey;
		// Removes this station from blockable list as it is added to a blocking region
		blockableStations.remove(stationKey);
		save = true;
		return true;
	}

	/**
	 * Sets the name of a blocking region
	 *
	 * @param regionKey Search's key for region
	 * @param name      name of the blocking region
	 */
	@Override
	public void setRegionName(Object regionKey, String name) {
		if (blockingRegionsKeyset.contains(regionKey)) {
			if (!((BlockingRegionData) blockingDataHM.get(regionKey)).name.equals(name)) {
				save = true;
			}
			((BlockingRegionData) blockingDataHM.get(regionKey)).name = name;
		}
	}

	/**
	 * Returns the name of a blocking region
	 *
	 * @param regionKey Search's key for region
	 * @return name of the blocking region
	 */
	@Override
	public String getRegionName(Object regionKey) {
		if (blockingRegionsKeyset.contains(regionKey)) {
			return ((BlockingRegionData) blockingDataHM.get(regionKey)).name;
		} else {
			return null;
		}
	}

	/**
	 * Sets the type of a blocking region
	 *
	 * @param regionKey Search's key for region
	 * @param type      type of the blocking region
	 */
	@Override
	public void setRegionType(Object regionKey, String type) {
		if (blockingRegionsKeyset.contains(regionKey)) {
			if (!((BlockingRegionData) blockingDataHM.get(regionKey)).type.equals(type)) {
				save = true;
			}
			((BlockingRegionData) blockingDataHM.get(regionKey)).type = type;
		}
	}

	/**
	 * Returns the type of a blocking region
	 *
	 * @param regionKey Search's key for region
	 * @return type of the blocking region
	 */
	@Override
	public String getRegionType(Object regionKey) {
		if (blockingRegionsKeyset.contains(regionKey)) {
			return ((BlockingRegionData) blockingDataHM.get(regionKey)).type;
		} else {
			return null;
		}
	}

	/**
	 * Tells if a station can be added to specified blocking region
	 * (all stations that creates or destroy jobs cannot be added)
	 * @param regionKey  Search's key for region
	 * @param stationKey Search's key for station
	 * @return true if station can be put inside specified region
	 */
	@Override
	public boolean canRegionStationBeAdded(Object regionKey, Object stationKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !stationsKeyset.contains(stationKey)) {
			return false;
		}
		StationData sd = (StationData) stationDataHM.get(stationKey);
		return (sd.blockingRegion == null || sd.blockingRegion == regionKey) && canStationTypeBeBlocked(sd.type);
	}

	/**
	 * Tells if a given station type can be added to a blocking region
	 * @param stationType type of station to be added
	 * @return true if station of that type can be added, false otherwise
	 */
	protected boolean canStationTypeBeBlocked(String stationType) {
		return !stationType.equals(CommonConstants.STATION_TYPE_SOURCE) && !stationType.equals(CommonConstants.STATION_TYPE_SINK)
				&& !stationType.equals(CommonConstants.STATION_TYPE_TERMINAL) && !stationType.equals(CommonConstants.STATION_TYPE_FORK)
				&& !stationType.equals(CommonConstants.STATION_TYPE_JOIN) && !stationType.equals(CommonConstants.STATION_TYPE_CLASSSWITCH)
				&& !stationType.equals(CommonConstants.STATION_TYPE_SEMAPHORE) && !stationType.equals(CommonConstants.STATION_TYPE_SCALER)
				&& !stationType.equals(CommonConstants.STATION_TYPE_TRANSITION);
	}

	/**
	 * Removes a station from a specified blocking region
	 *
	 * @param regionKey  Search's key for region
	 * @param stationKey Search's key for station
	 */
	@Override
	public void removeRegionStation(Object regionKey, Object stationKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !stationsKeyset.contains(stationKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).stations.remove(stationKey);
		((StationData) stationDataHM.get(stationKey)).blockingRegion = null;
		// If this station can be blocked, adds it to blockable stations vector
		if (canStationTypeBeBlocked(((StationData) stationDataHM.get(stationKey)).type)) {
			blockableStations.add(stationKey);
		}
		save = true;
	}

	/**
	 * Returns the entire set of blocking region keys
	 *
	 * @return the entire set of blocking region keys
	 */
	@Override
	public Vector<Object> getRegionKeys() {
		return blockingRegionsKeyset;
	}

	/**
	 * Sets a customer number constraint for a given region and an user class
	 *
	 * @param regionKey       search's key for blocking region
	 * @param classKey        search's key for customer class
	 * @param maxJobsPerClass maximum number of allowed customer. -1 means infinity.
	 */
	@Override
	public void setRegionClassCustomerConstraint(Object regionKey, Object classKey, Integer maxJobsPerClass) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return;
		}
		if (!((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).maxJobs.equals(maxJobsPerClass)) {
			save = true;
		}
		((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).maxJobs = maxJobsPerClass;
	}

	/**
	 * Sets a drop rule for a given region and an user class
	 *
	 * @param regionKey search's key for blocking region
	 * @param classKey  search's key for customer class
	 * @param drop      true if jobs of specified class can be dropped, false otherwise
	 */
	@Override
	public void setRegionClassDropRule(Object regionKey, Object classKey, Boolean drop) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return;
		}
		if (!((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).drop.equals(drop)) {
			save = true;
		}
		((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).drop = drop;
	}

	/**
	 * Sets a global customer number constraint for a given region
	 *
	 * @param regionKey       search's key for blocking region
	 * @param maxJobs maximum number of allowed customer. -1 means infinity.
	 */
	@Override
	public void setRegionCustomerConstraint(Object regionKey, Integer maxJobs) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (!((BlockingRegionData) blockingDataHM.get(regionKey)).maxJobs.equals(maxJobs)) {
			save = true;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).maxJobs = maxJobs;
	}

	/**
	 * Gets customer number constraint for a given region and an user class
	 *
	 * @param regionKey search's key for blocking region
	 * @param classKey  search's key for customer class
	 * @return maximum number of allowed customer. -1 means infinity.
	 */
	@Override
	public Integer getRegionClassCustomerConstraint(Object regionKey, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return null;
		}
		return ((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).maxJobs;
	}

	/**
	 * Gets drop rule for a given region and an user class
	 *
	 * @param regionKey search's key for blocking region
	 * @param classKey  search's key for customer class
	 * @return true if jobs of specified class can be dropped, false otherwise
	 */
	@Override
	public Boolean getRegionClassDropRule(Object regionKey, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return null;
		}
		return ((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).drop;
	}

	/**
	 * Gets global customer number constraint for a given region
	 *
	 * @param regionKey search's key for blocking region
	 * @return maximum number of allowed customer. -1 means infinity.
	 */
	@Override
	public Integer getRegionCustomerConstraint(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).maxJobs;
	}

	/**
	 * Returns a list of every station inside a blocking region
	 *
	 * @param regionKey search's key for given blocking region
	 * @return a set with every station inside given blocking region
	 */
	@Override
	public Set<Object> getBlockingRegionStations(Object regionKey) {
		if (blockingRegionsKeyset.contains(regionKey)) {
			return ((BlockingRegionData) blockingDataHM.get(regionKey)).stations;
		} else {
			return null;
		}
	}

	/**
	 * Gets blocking region of a given station
	 *
	 * @param stationKey search's key for given station
	 * @return search's key for its blocking region or null if it is undefined
	 */
	@Override
	public Object getStationBlockingRegion(Object stationKey) {
		if (stationsKeyset.contains(stationKey)) {
			return ((StationData) stationDataHM.get(stationKey)).blockingRegion;
		} else {
			return null;
		}
	}

	/**
	 * Gets a vector with every station that can be added to a blocking region
	 *
	 * @return a vector of search's keys of every station that can be added
	 *         to a blocking region
	 */
	@Override
	public Vector<Object> getBlockableStationKeys() {
		return blockableStations;
	}

	/**
	 * Adds default values for newly created class
	 * @param classKey search's key for created class
	 */
	protected void addBlockingDetailsForClass(Object classKey) {
		Vector<Object> blockingKeys = getRegionKeys();
		for (int i = 0; i < blockingKeys.size(); i++) {
			blockingDetailsBDM.put(classKey, blockingKeys.get(i), new BlockingClassData(Defaults.getAsInteger("blockingMaxJobsPerClass"),
					Defaults.getAsBoolean("blockingDropPerClass"), Defaults.getAsInteger("weight"),Defaults.getAsInteger("size")));
		}
	}

	/**
	 * Adds default values for newly created blocking regions
	 * @param blockingRegionKey search's key for created blocking region
	 */
	protected void addBlockingDetails(Object blockingRegionKey) {
		Vector<Object> classKeys = getClassKeys();
		for (int i = 0; i < classKeys.size(); i++) {
			blockingDetailsBDM.put(classKeys.get(i), blockingRegionKey, new BlockingClassData(Defaults.getAsInteger("blockingMaxJobsPerClass"),
					Defaults.getAsBoolean("blockingDropPerClass"), Defaults.getAsInteger("weight"), Defaults.getAsInteger("size")));
		}
	}

	/**
	 * Deletes blocking region details about a certain class, given its search key
	 */
	protected void deleteBlockingDetailsForClass(Object classKey) {
		blockingDetailsBDM.remove(classKey, BDMap.X);
	}

	/**
	 * Deletes blocking region details from repository, given station search key.
	 */
	protected void deleteBlockingDetails(Object blockingRegionKey) {
		blockingDetailsBDM.remove(blockingRegionKey, BDMap.Y);
	}

	@Override
	public Integer getRegionMemorySize(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).maxMemory;
	}

	@Override
	public void setRegionMemorySize(Object regionKey, Integer memorySize) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (!((BlockingRegionData) blockingDataHM.get(regionKey)).maxMemory.equals(memorySize)) {
			save = true;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).maxMemory = memorySize;
	}

	@Override 
	public Integer getRegionClassWeight(Object regionKey, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return null;
		}
		return ((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).weight;
	}

	@Override
	public void setRegionClassWeight(Object regionKey, Object classKey, Integer weight) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return;
		}
		if (!((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).weight.equals(weight)) {
			save = true;
		}
		((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).weight = weight;
	}

	@Override 
	public Integer getRegionClassSize(Object regionKey, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return null;
		}
		return ((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).size;
	}

	@Override
	public void setRegionClassSize(Object regionKey, Object classKey, Integer size) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return;
		}
		if (!((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).size.equals(size)) {
			save = true;
		}
		((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).size = size;
	}

	@Override
	public List<BlockingGroupData> getRegionGroupList(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList;
	}

	@Override
	public void addRegionGroup(Object regionKey, String name, Integer capacity, String strategy) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.add(new BlockingGroupData(name, capacity, strategy));
		save = true;
	}

	@Override
	public void deleteRegionGroup(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.remove(index);
		save = true;
	}

	@Override
	public void deleteAllRegionGroups(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.clear();
		save = true;
	}

	@Override
	public String getRegionGroupName(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).name;
	}

	@Override
	public void setRegionGroupName(Object regionKey, int index, String name) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).name = name;
		save = true;
	}

	@Override
	public Integer getRegionGroupCapacity(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).maxJobs;
	}

	@Override
	public void setRegionGroupCapacity(Object regionKey, int index, Integer maxJobs) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).maxJobs = maxJobs;
		save = true;
	}

	@Override
	public String getRegionGroupStrategy(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).strategy;
	}

	@Override
	public void setRegionGroupStrategy(Object regionKey, int index, String strategy) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).strategy = strategy;
		save = true;
	}

	@Override
	public List<Object> getRegionGroupClassList(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).classList;
	}

	@Override
	public void addClassIntoRegionGroup(Object regionKey, int index, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).classList.contains(classKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).classList.add(classKey);
		save = true;
	}

	@Override
	public void deleteClassFromRegionGroup(Object regionKey, int index, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		if (!((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).classList.contains(classKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).classList.remove(classKey);
		save = true;
	}

	@Override
	public void deleteAllClassesFromRegionGroup(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).classList.clear();
		save = true;
	}

	// --- end Blocking Region Definition ------------------------------------------------------------

	/**This class packs altogether station parameters. This can be useful for storing
	 * inside an hash map or other data structures. Data contained inside this class are
	 * defined once per each station. Data which are defined for a couple of one station
	 * and one class (such as service times distribution) or for a couple of stations (
	 * such as connections) need to be stored in other type of data structure and should not
	 * be stored inside this class*/
	protected class StationData {
		public String queueStrategy;
		public String name;
		public String type;
		public Integer numOfServers;
		public Integer queueCapacity;
		public Integer forkBlock;
		public Object loggerParameters;
		/** Reference to owner blocking station or null */
		public Object blockingRegion;
		public Boolean isSimplifiedFork;

		public StationData(String name, String type, Integer numOfServers, Integer queueCapacity) {
			this.name = name;
			this.type = type;
			this.numOfServers = numOfServers;
			this.queueCapacity = queueCapacity;
			this.loggerParameters = null;
			this.isSimplifiedFork = Boolean.parseBoolean(Defaults.get("isSimplifiedFork"));
		}
	}

	/**This class packs altogether customer class parameters. This can be useful for
	 * storing them inside an hash map or other data structures.*/
	protected class ClassData {
		public String name;
		public int type;
		public int priority;
		public Integer population;
		public Object distribution;
		public Object refSource = null;

		public ClassData(String name, int type, int priority, Integer population, Object distribution) {
			this.name = name;
			this.type = type;
			this.priority = priority;
			this.population = population;
			this.distribution = distribution;
		}
	}

	/**
	 * This class is used to store blocking region data
	 */
	protected class BlockingRegionData {
		public String name;
		public String type;
		public Integer maxJobs;
		public Integer maxMemory;
		public List<BlockingGroupData> GroupList;

		/** Reference to owned stations */
		public HashSet<Object> stations;

		public BlockingRegionData(String name, String type, Integer maxJobs, Integer maxMemory) {
			this.name = name;
			this.type = type;
			this.maxJobs = maxJobs;
			this.maxMemory = maxMemory;
			stations = new HashSet<Object>();
			GroupList = new ArrayList<BlockingGroupData>();
		}
	}

	/**
	 * This class is used to store blocking region / group data
	 */
	public class BlockingGroupData {
		public String name;
		public Integer maxJobs;
		public String strategy;
		public List<Object> classList;

		public BlockingGroupData(String name,Integer maxJobs,String strategy) {
			this.name = name;
			this.maxJobs = maxJobs;
			this.strategy = strategy;
			classList = new ArrayList<Object>();
		}
	}

	/**
	 * This class is used to store blocking region / class data
	 */
	protected class BlockingClassData {
		public Integer maxJobs;
		public Boolean drop;
		public Integer weight;
		public Integer size;

		public BlockingClassData(Integer maxJobs, Boolean drop, Integer weight, Integer size) {
			this.maxJobs = maxJobs;
			this.drop = drop;
			this.weight = weight;
			this.size = size;
		}
	}

	/**This class represents a connection between two stations. Two boolean parameters
	 * are defined. The first, isConnected, tells whether the two stations are connected
	 * The second, isConnectable, tells whether these two stations are connectable.*/
	protected class Connection {
		public boolean isConnected = false;
		public boolean isConnectable = false;

		public Connection(boolean isConnectable, boolean isConnected) {
			this.isConnectable = isConnectable;
			this.isConnected = isConnected;
		}
	}

	/**This class contains all of the data which can be defined for a combination of
	 * class and station. These data include queue policy, service time distribution
	 * and routing strategy*/
	protected class StationClassData {
		public String queueStrategy;
		public Object serviceDistribution;
		public Object routingStrategy;
		public Object classSwitchProb; //this is a row of the cs matrix
		public Integer preload;
		public String dropRule;
		//bpmn
		public Object forkingStrategy;
		public Object joinStrategy;
		public Object semaphoreStrategy;
		public Object enablingCondition;
		public Object inhibitingCondition;
		public Object firingOutcome;
		public Integer firingPriority;
		public Integer firingWeight;

		public StationClassData(String queueStrategy, Object serviceDistribution, Object routingStrategy,
				Object classSwitchProb, String dropRule, Object forkingStrategy, Object joinStrategy,
				Object semaphoreStrategy, Object enablingCondition, Object inhibitingCondition, Object firingOutcome,
				Integer firingPriority, Integer firingWeight) {
			this.queueStrategy = queueStrategy;
			this.serviceDistribution = serviceDistribution;
			this.routingStrategy = routingStrategy;
			this.classSwitchProb = classSwitchProb;
			this.preload = new Integer(0);
			this.dropRule = dropRule;
			this.forkingStrategy = forkingStrategy;
			this.joinStrategy = joinStrategy;
			this.semaphoreStrategy = semaphoreStrategy;
			this.enablingCondition = enablingCondition;
			this.inhibitingCondition = inhibitingCondition;
			this.firingOutcome = firingOutcome;
			this.firingPriority = firingPriority;
			this.firingWeight = firingWeight;
		}
	}

	/**This class contains all of the parameters to define a simulation measure, e.g.
	 * reference userClass, reference station, type (Throughput, Residence Time, ecc)
	 * precision, confidence interval */
	protected class MeasureData {
		public Object stationKey;
		public Object classKey;
		public String type;
		public Double precision;
		public Double alpha;
		public boolean log; 

		public MeasureData(Object station, Object userClass, String measureType, Double alpha, Double precision, boolean log) {
			stationKey = station;
			classKey = userClass;
			type = measureType;
			this.precision = precision;
			this.alpha = alpha;
			this.log = log ; 
		}
	}

	protected class LoggerGlobalParameters {
		public String path;
		public String delimiter;
		public String decimalSeparator;
		public Integer autoAppendMode;

		LoggerGlobalParameters() {
			path = MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR);
			delimiter = Defaults.get("loggerDelimiter");
			decimalSeparator = Defaults.get("loggerDecimalSeparator");
			autoAppendMode = Defaults.getAsInteger("loggerAutoAppend");
		}
	}

	@Override
	public boolean isSinkProbabilityUpdated() {
		return sinkProbabilityUpdate;
	}

	@Override
	public void setSinkProbabilityUpdatedVar(boolean param) {
		sinkProbabilityUpdate = param;
	}

	@Override
	public Vector<String> getsinkProbabilityUpdateClasses() {		
		return sinkProbabilityUpdateClasses;
	}

	@Override
	public Vector<String> getsinkProbabilityUpdateStations() {		
		return sinkProbabilityUpdateStations;
	}

	@Override
	public void resetSinkProbabilityUpdateStations() {
		sinkProbabilityUpdateStations = new Vector<String>();
	}

	@Override
	public void resetSinkProbabilityUpdateClasses() {
		sinkProbabilityUpdateClasses = new Vector<String>();
	}

	/**
	 *	Returns the cell (<code>classInKey</code>, <code>classOutKey</code>) of the class
	 *  switch matrix of station <code>stationKey</code>.
	 */
	@Override
	public float getClassSwitchMatrix(Object stationKey, Object classInKey, Object classOutKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classInKey, stationKey);
		ClassSwitch row = (ClassSwitch) current.classSwitchProb;
		return row.getValue(classInKey, classOutKey);
	}

	/**
	 *  Sets the cell (<code>classInKey</code>, <code>classOutKey</code>) of the class
	 *  switch matrix of station <code>stationKey</code>.
	 */
	@Override
	public void setClassSwitchMatrix(Object stationKey, Object classInKey, Object classOutKey, float val) {
		if (val != getClassSwitchMatrix(stationKey, classInKey, classOutKey))
			save = true;
		StationClassData current = (StationClassData) stationDetailsBDM.get(classInKey, stationKey);
		ClassSwitch row = (ClassSwitch) current.classSwitchProb;
		row.setValue(classInKey, classOutKey, val);
	}

	/**
	 * Tells if a given station type is preloadable
	 * @param stationType type of station
	 * @return true if station of that type is preloadable, false otherwise
	 */
	protected boolean isStationTypePreloadable(String stationType) {
		return !stationType.equals(STATION_TYPE_SOURCE) && !stationType.equals(STATION_TYPE_SINK)
				&& !stationType.equals(STATION_TYPE_TERMINAL) && !stationType.equals(STATION_TYPE_JOIN)
				&& !stationType.equals(STATION_TYPE_SEMAPHORE) && !stationType.equals(STATION_TYPE_SCALER)
				&& !stationType.equals(STATION_TYPE_TRANSITION);
	}

	/**
	 *  Returns the entry <code>stationInKey</code> of the enabling condition for
	 *  station <code>stationKey</code> and class <code>classKey</code>.
	 */
	public Integer getEnablingCondition(Object stationKey, Object classKey, Object stationInKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		TransitionVector vector = (TransitionVector) current.enablingCondition;
		return vector.getValue(stationInKey);
	}

	/**
	 *  Sets the entry <code>stationInKey</code> of the enabling condition for
	 *  station <code>stationKey</code> and class <code>classKey</code>.
	 */
	@Override
	public void setEnablingCondition(Object stationKey, Object classKey, Object stationInKey, Integer value) {
		if (!value.equals(getEnablingCondition(stationKey, classKey, stationInKey)))
			save = true;
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		TransitionVector vector = (TransitionVector) current.enablingCondition;
		vector.setValue(stationInKey, value);
	}

	/**
	 *  Returns the entry <code>stationInKey</code> of the inhibiting condition for
	 *  station <code>stationKey</code> and class <code>classKey</code>.
	 */
	public Integer getInhibitingCondition(Object stationKey, Object classKey, Object stationInKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		TransitionVector vector = (TransitionVector) current.inhibitingCondition;
		return vector.getValue(stationInKey);
	}

	/**
	 *  Sets the entry <code>stationInKey</code> of the inhibiting condition for
	 *  station <code>stationKey</code> and class <code>classKey</code>.
	 */
	@Override
	public void setInhibitingCondition(Object stationKey, Object classKey, Object stationInKey, Integer value) {
		if (!value.equals(getEnablingCondition(stationKey, classKey, stationInKey)))
			save = true;
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		TransitionVector vector = (TransitionVector) current.inhibitingCondition;
		vector.setValue(stationInKey, value);
	}

	/**
	 *  Returns the entry <code>stationOutKey</code> of the firing outcome for
	 *  station <code>stationKey</code> and class <code>classKey</code>.
	 */
	@Override
	public Integer getFiringOutcome(Object stationKey, Object classKey, Object stationOutKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		TransitionVector vector = (TransitionVector) current.firingOutcome;
		return vector.getValue(stationOutKey);
	}

	/**
	 *  Sets the entry <code>stationOutKey</code> of the firing outcome for
	 *  station <code>stationKey</code> and class <code>classKey</code>.
	 */
	@Override
	public void setFiringOutcome(Object stationKey, Object classKey, Object stationOutKey, Integer value) {
		if (!value.equals(getFiringOutcome(stationKey, classKey, stationOutKey)))
			save = true;
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		TransitionVector vector = (TransitionVector) current.firingOutcome;
		vector.setValue(stationOutKey, value);
	}

	/**
	 * Returns the firing priority for station <code>stationKey</code> and class
	 * <code>classKey</code>.
	 */
	@Override
	public Integer getFiringPriority(Object stationKey, Object classKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null) {
			return current.firingPriority;
		} else {
			return null;
		}
	}

	/**
	 * Sets the firing priority for station <code>stationKey</code> and class
	 * <code>classKey</code>.
	 */
	@Override
	public void setFiringPriority(Object stationKey, Object classKey, Integer value) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null) {
			if (!current.firingPriority.equals(value)) {
				save = true;
			}
			current.firingPriority = value;
		}
	}

	/**
	 * Returns the firing weight for station <code>stationKey</code> and class
	 * <code>classKey</code>.
	 */
	@Override
	public Integer getFiringWeight(Object stationKey, Object classKey) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null) {
			return current.firingWeight;
		} else {
			return null;
		}
	}

	/**
	 * Sets the firing weight for station <code>stationKey</code> and class
	 * <code>classKey</code>.
	 */
	@Override
	public void setFiringWeight(Object stationKey, Object classKey, Integer value) {
		StationClassData current = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (current != null) {
			if (!current.firingWeight.equals(value)) {
				save = true;
			}
			current.firingWeight = value;
		}
	}

}
