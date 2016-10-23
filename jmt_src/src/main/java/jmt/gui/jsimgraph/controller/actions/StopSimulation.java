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

package jmt.gui.jsimgraph.controller.actions;

import java.awt.event.ActionEvent;

import jmt.gui.jsimgraph.controller.Mediator;

/**
 * <p>Title: Stop Simulation Action</p>
 * <p>Description: Action performed when pressing the Stop Simulation... menu item.</p>
 *
 * @author Bertoli Marco
 *         Date: 07-set-2005
 *         Time: 14.19.21

 */
public class StopSimulation extends AbstractJmodelAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Defines an <code>Action</code> object with a default
	 * description string and default icon.
	 */
	public StopSimulation(Mediator mediator) {
		super("Stop Simulation", "Stop", mediator);
		putValue(SHORT_DESCRIPTION, "Stop simulation");
		//		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		setEnabled(false);
	}

	/**
	 * Invoked when an action occurs.
	 */
	public void actionPerformed(ActionEvent e) {
		mediator.stopSimulation();
	}
}
