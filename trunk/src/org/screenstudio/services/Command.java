/*
 * Copyright (C) 2014 patrick
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.screenstudio.services;

import org.w3c.dom.Node;

/**
 *
 * @author patrick
 */
public class Command {
    private String commandLine = "";
    private String name = "";
    public Command(Node xml){
        name = xml.getAttributes().getNamedItem("name").getNodeValue();
        commandLine = xml.getTextContent().trim();
    }
    
    @Override
    public String toString(){
        return getName();
    }

    /**
     * @return the commandLine
     */
    public String getCommandLine() {
        return commandLine;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
}
