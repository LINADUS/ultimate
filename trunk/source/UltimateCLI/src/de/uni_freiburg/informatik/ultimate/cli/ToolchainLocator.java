/*
 * Copyright (C) 2016 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2016 University of Freiburg
 *
 * This file is part of the ULTIMATE CLI plug-in.
 *
 * The ULTIMATE CLI plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE CLI plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE CLI plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE CLI plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE CLI plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.PluginType;
import de.uni_freiburg.informatik.ultimate.core.lib.toolchain.RunDefinition;
import de.uni_freiburg.informatik.ultimate.core.model.ICore;
import de.uni_freiburg.informatik.ultimate.core.model.IToolchainData;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;

/**
 *
 * This class searches for feasible toolchain files in the current working directory and loads them s.t. we can filter
 * settings based on their contents.
 *
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public class ToolchainLocator {

	private final File mDir;
	private final ICore<RunDefinition> mCore;
	private final ILogger mLogger;

	private List<IToolchainData<RunDefinition>> mFoundToolchains;

	public ToolchainLocator(final File dirOrFile, final ICore<RunDefinition> core, final ILogger logger) {
		mDir = dirOrFile;
		mCore = core;
		mLogger = logger;
	}

	public Predicate<String> createFilterForAvailableTools() {
		Predicate<String> rtr = a -> false;

		// the current cli controller and the core are always allowed
		rtr = rtr.or(a -> Activator.PLUGIN_ID.equalsIgnoreCase(a));
		rtr = rtr.or(a -> de.uni_freiburg.informatik.ultimate.core.coreplugin.Activator.PLUGIN_ID.equalsIgnoreCase(a));

		final List<IToolchainData<RunDefinition>> availableToolchains = locateToolchains();
		final Set<String> availablePluginIds = availableToolchains.stream()
				.flatMap(a -> a.getToolchain().getToolchain().getPluginOrSubchain().stream())
				.filter(a -> a instanceof PluginType).map(a -> ((PluginType) a).getId().toLowerCase())
				.collect(Collectors.toSet());
		rtr = rtr.or(a -> availablePluginIds.contains(a.toLowerCase()));
		return rtr;
	}

	private List<IToolchainData<RunDefinition>> locateToolchains() {
		if (mFoundToolchains != null) {
			return Collections.unmodifiableList(mFoundToolchains);
		}

		if (mLogger.isDebugEnabled()) {
			mLogger.debug("Trying to read toolchain files from " + mDir);
		}

		if (!mDir.exists()) {
			return Collections.emptyList();
		}

		final File[] xmlFiles = mDir.listFiles((file, name) -> name.endsWith(".xml"));
		if (xmlFiles.length == 0) {
			return Collections.emptyList();
		}

		mFoundToolchains = new ArrayList<>();
		for (final File xmlFile : xmlFiles) {
			try {
				final IToolchainData<RunDefinition> toolchain = mCore.createToolchainData(xmlFile.getAbsolutePath());
				mFoundToolchains.add(toolchain);
			} catch (final FileNotFoundException e) {
				mLogger.error("Could not find file: " + e.getMessage());
			} catch (final JAXBException e) {
			} catch (final SAXException e) {
			}
		}
		return Collections.unmodifiableList(mFoundToolchains);
	}

}
