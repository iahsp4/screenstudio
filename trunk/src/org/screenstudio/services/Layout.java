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
public class Layout {
    private String type = "";
    private String location="";
    private String value="";
    
    protected Layout(Node xml){
        type = xml.getAttributes().getNamedItem("type").getNodeValue();
        location =xml.getAttributes().getNamedItem("location").getNodeValue();
        value =xml.getAttributes().getNamedItem("value").getNodeValue();
    }

    /**
     * @return the name
     */
    public String getType() {
        return type;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString(){
        return location;
    }
    
}
