/*
 * Copyright (C) 2017 John Garner <segfaultcoredump@gmail.com>
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
package com.pikatimer.results.reports;

import com.pikatimer.event.Event;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.results.ProcessedResult;
import com.pikatimer.results.RaceReport;
import com.pikatimer.results.RaceReportType;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.Pace;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class OverallHTML5 implements RaceReportType{
    Race race;

    Boolean showDQ = true;
    Boolean inProgress = false;
    Boolean showSplits = false;
    Boolean showSegments = false;
    Boolean showSegmentPace = false;
    Boolean showDNF = true;
    Boolean showPace = true;
    Boolean showGun = true;

    Map<String,Boolean> supportedOptions = new HashMap();
    
    public OverallHTML5(){
        supportedOptions.put("showDQ", true);
        supportedOptions.put("inProgress", false);
        supportedOptions.put("showSplits", false);
        supportedOptions.put("showSegments", true);
        supportedOptions.put("showSegmentPace", false);
        supportedOptions.put("showDNF", false);
        supportedOptions.put("showPace", true);
        supportedOptions.put("showGun", true);
        supportedOptions.put("hideCustomHeaders", false);
    }
    
    @Override
    public void init(Race r) {
        race = r;
    }

    @Override
    public Boolean optionSupport(String feature) {
        return supportedOptions.containsKey(feature);
    }
    
    @Override
    public String process(List<ProcessedResult> prList, RaceReport rr) {
        System.out.println("OverallHTML.process() Called... ");
        String report = new String();
        
        race = rr.getRace();
        OverallJSON json = new OverallJSON();
        json.init(race);
        
        
        Event event = Event.getInstance();  // fun with singletons... 
        
        supportedOptions.keySet().forEach(k -> supportedOptions.put(k, rr.getBooleanAttribute(k)));


        showDQ = supportedOptions.get("showDQ");
        inProgress = supportedOptions.get("inProgress");
        showSplits = supportedOptions.get("showSplits");
        showSegments = supportedOptions.get("showSegments");
        showSegmentPace = supportedOptions.get("showSegmentPace");
        showDNF = supportedOptions.get("showDNF");
        showPace = supportedOptions.get("showPace");
        showGun = supportedOptions.get("showGun");
        
        Boolean customHeaders = race.getBooleanAttribute("useCustomHeaders");
        Boolean textOnlyHeaders = race.getBooleanAttribute("textOnlyHeaders");
        if (customHeaders == null || (customHeaders == true && supportedOptions.get("hideCustomHeaders"))) customHeaders = false;
        if (customHeaders && textOnlyHeaders == null) textOnlyHeaders = false;
        
        String dispFormat = race.getStringAttribute("TimeDisplayFormat");
        String roundMode = race.getStringAttribute("TimeRoundingMode");
        Pace pace = Pace.valueOf(race.getStringAttribute("PaceDisplayFormat"));
        
        Integer dispFormatLength;  // add a space
        if (dispFormat.contains("[HH:]")) dispFormatLength = dispFormat.length()-1; // get rid of the two brackets and add a space
        else dispFormatLength = dispFormat.length()+1;
        
        Duration cutoffTime = Duration.ofNanos(race.getRaceCutoff());
        String cutoffTimeString = DurationFormatter.durationToString(cutoffTime, dispFormat, roundMode);
        
       
        report += "<HTML> " +  System.lineSeparator();
        report += "  <HEAD> " +  System.lineSeparator();
        
        report += "<TITLE> " + event.getEventName();
        if (RaceDAO.getInstance().listRaces().size() > 1) 
            report += " " + race.getRaceName() ;
        report += " " + event.getLocalEventDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) + "</TITLE>" + System.lineSeparator();
        report += System.lineSeparator();
        report += "<meta name=\"viewport\" content=\"width=device-width; initial-scale=1.0; maximum-scale=1.0; minimum-scale=1.0; user-scalable=0;\" />\n";
        
// Google Analytics
        if (customHeaders && race.getStringAttribute("GACode") != null && !race.getStringAttribute("GACode").isEmpty()) {
            report +=   "<!-- Google Analytics -->\n" +
                        "<script>\n" +
                        "window.ga=window.ga||function(){(ga.q=ga.q||[]).push(arguments)};ga.l=+new Date;\n" +
                        "ga('create', '" + race.getStringAttribute("GACode") + "', 'auto');\n" +
                        "ga('send', 'pageview');\n" +
                        "</script>\n" +
                        "<script async src='https://www.google-analytics.com/analytics.js'></script>\n" +
                        "<!-- End Google Analytics -->\n" ;
                        
            report += System.lineSeparator();
        }
        
        report +=   "<!-- Stylesheets / JS Includes-->\n" +
                    "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.datatables.net/v/dt/jq-2.2.4/dt-1.10.13/fh-3.1.2/r-2.1.0/sc-1.4.2/datatables.min.css\"/>\n" +
                    " \n" +
                    "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/v/dt/jq-2.2.4/dt-1.10.13/fh-3.1.2/r-2.1.0/sc-1.4.2/datatables.min.js\"></script>\n" +
                    " \n" +
                    "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/plug-ins/1.10.12/sorting/natural.js\"></script>\n";
        
        // our inline CSS
        report +=   "<link href=\"https://fonts.googleapis.com/css?family=Source+Sans+Pro|Open+Sans\" rel=\"stylesheet\">\n" +
                    "<style>\n" +
                    "table.dataTable.display tbody tr.child {\n" +
                    "    background: white;\n" +
                    "}\n" +
                    "table.dataTable.display tbody tr.child:hover {\n" +
                    "    background: white !important;\n" +
                    "}" +
                    ".row{padding-bottom: 15px;}\n" +
                    ".row::after {\n" +
                    "    content: \"\";\n" +
                    "    clear: both;\n" +
                    "    display: table;\n" +
                    "}\n" +
                    ".right {text-align: right;}" +
                    ".event-info {font-family: 'Open Sans'; font-size: 40px; text-align: center;}\n" +
                    ".in-progress {font-family: 'Open Sans'; font-size: 40px; text-align: center; color: red;}\n" +
                    ".participant {float: left; padding-right: 15px;}\n" +
                    ".overall {float: left; padding-right: 15px;}\n" +
                    ".part-name {font-family: 'Source Sans Pro'; font-size: 36px; text-align: left; white-space: pre-wrap;}\n" +
                    ".part-stats {font-family: 'Source Sans Pro'; font-size: 20px; text-align: left; white-space: pre-wrap;}\n" +
                    ".finish-time {font-family: 'Source Sans Pro'; font-size: 36px; text-align: left;}\n" +
                    ".finish-stats {font-family: 'Source Sans Pro'; font-size: 20px; text-align: left; white-space: pre-wrap;}\n" +
                    ".segment {float: left; padding-right: 15px; padding-bottom: 15px}\n" +
                    ".segment-head {font-family: 'Source Sans Pro'; font-size: 24px; text-align: left; white-space: pre-wrap;}\n" +
                    ".segment-time {font-family: 'Source Sans Pro'; font-size: 20px; text-align: left; white-space: pre-wrap;}\n" +
                    ".segment-stats {font-family: 'Source Sans Pro'; font-size: 18px; text-align: left; white-space: pre-wrap;}\n" +
                    ".split {float: left; padding-right: 15px; padding-bottom: 15px}\n" +
                    ".split-head {font-family: 'Source Sans Pro'; font-size: 24px; text-align: left; white-space: pre-wrap;}\n" +
                    ".split-time {font-family: 'Source Sans Pro'; font-size: 16px; text-align: left; white-space: pre-wrap;}\n" +
                    "@media only screen and (max-width: 600px) {\n" +
                    "    /* For mobile phones: */\n" +
                    "    .participant {width: 100%; padding-right: 0px; }\n" +
                    "    .overall {width: 100%; padding-right: 0px;}\n" +
                    "    .segment {width: 100%; padding-right: 0px;}\n" +
                    "    .split {width: 100%; padding-right: 0px;}\n" +
                    "}\n" +
                    "</style>\n";
        // Custom CSS 
        if (customHeaders && race.getStringAttribute("CSSUrl") != null && !race.getStringAttribute("CSSUrl").isEmpty()) {
            report += "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + race.getStringAttribute("CSSUrl") + "\"/>\n";
            report += System.lineSeparator();
        }
        report += "<!-- End Stylesheets / JS Indludes-->\n";

        // DataTables
        report += "<script type=\"text/javascript\" class=\"init\">\n" +
                    "	\n" +
                    " var resultsData = " + json.process(prList, rr) +
                    "\n" +
                    "$(document).ready(function() {\n" +
                    "var search = \"\";\n" +
                    "	if ( window.location.hash !== \"\" ) {\n" +
                    "		search = window.location.hash.substring( 1 );\n" +
                    "	}" +
                    "var oTable = $('#results').DataTable({\n" +
                        "   data: resultsData,\n" +
                        "        \"columns\": [\n" +
                        "           { \"data\": null, \"defaultContent\": \"\", className: 'control', orderable: false, targets:   0 },\n" +
                        "           { \"data\": \"oa_place\" },\n" +
                        "           { \"data\": \"sex_place\" },\n" +
                        "           { \"data\": \"ag_place\" },\n" +
                        "           { \"data\": \"bib\" },\n" +
                        "           { \"data\": \"age\" },\n" +
                        "           { \"data\": \"sex\" },\n" +
                        "           { \"data\": \"ag\" },\n" +
                        "           { \"data\": \"full_name\" },\n" +
                        "           { \"data\": \"city\" },\n" +
                        "           { \"data\": \"state\" },\n" +
                        "           { \"data\": \"country\" },\n";
            if (showSplits) {
                for (int i = 2; i < race.splitsProperty().size(); i++) {
                    report += "           { \"data\": \"splits.split_"+ race.splitsProperty().get(i-1).getSplitName() + "\", \n" +
                                "				\"render\": {\n" +
                                "					_: 'display',\n" +
                                "					sort: 'sort'\n" +
                                "				} \n" +
                                "			},\n"; //+ race.splitsProperty().get(i-1).getSplitName() + "</th>" +  System.lineSeparator();
                }
            }
            if (showSegments) {
                final StringBuilder chars = new StringBuilder();
                Integer dispLeg = dispFormatLength;
                race.getSegments().forEach(seg -> {
                    chars.append("           { \"data\": \"segments.segment_" +  seg.getSegmentName() + "\", \n" +
                                "				\"render\": {\n" +
                                "					_: 'display',\n" +
                                "					sort: 'sort'\n" +
                                "				} \n" +
                                "			},\n");
                    if (showSegmentPace) chars.append("           { \"data\": \"segments.segment_" +  seg.getSegmentName() + ".pace\" },\n"); // pace.getFieldWidth()+1
                });
                report += chars.toString();
            }
            report +=   "           { \"data\": { \n" +
                        "				\"_\": \"finish_display\",\n" +
                        "				\"sort\": \"finish_sort\"}\n" +
                        "			},\n";
            
            if (showGun) report +=  "           { \"data\": { \n" +
                                    "				\"_\": \"gun_display\",\n" +
                                    "				\"sort\": \"finish_sort\"}\n" +
                                    "			},\n";
            if (showPace) report += "           { \"data\": \"finish_pace\" }\n";
            report +=   "        ],\n" +
                        "   \"oSearch\": { \"sSearch\": search },\n" + 
                        "    responsive: {\n" +
                        "            details: {\n" +
                        "                renderer: function ( api, rowIdx, columns ) {\n" +
                        "                var rData = api.row(rowIdx).data();\n" +
                        "                \n" +
                        "				var data = '<div class=\"detail\">';\n" +
                        "				data += '<div class=\"row\">';\n" +
                        "				data += '<div class=\"participant\">' // personal\n" +
                        "				data += '<div class=\"part-name\">' + rData.full_name + '</div>';\n" +
                        "				data += '<div class=\"part-stats\">Age: ' + rData.age + '   Sex: ' + rData.sex + '   AG: ' + rData.ag + '</div>';\n" +
                        "				data += '<div class=\"part-stats\">' + rData.city + ', ' + rData.state + '</div>';\n" +
                        "				data += '<div class=\"part-stats\">' + rData.country + '</div>';\n" +
                        "				data += '</div>'; // personal\n" +
                        "				data += '<div class=\"overall\">';// time\n" +
                        "                               if ( rData.oa_place == \"DQ\" ) {\n" +
                        "					data += '<div class=\"finish-time\">DISQUALIFIED</div>';\n" +
                        "					data += '<div class=\"finish-stats\">' + rData.note + '</div>';\n" +
                        "                               } else if ( rData.oa_place == \"DNF\" ) {\n" +
                        "					data += '<div class=\"finish-time\">Did Not Finish  :-( </div>';\n" +
                        "                               } else if ( rData.oa_place == \"Started\" ) {\n" +
                        "					data += '<div class=\"finish-time\">Started</div>';\n" +
                        "                                   data += '<div class=\"finish-stats\">Last Seen: ' + rData.last_seen + '</div>';\n" +
                        "                               } else if ( rData.oa_place == \"OCO\" ) {\n" +
                        "					data += '<div class=\"finish-time\">Over Cut Off  :-/</div>';\n" +
                        "                                   data += '<div class=\"finish-stats\"> Finish time: ' + rData.finish_display + '</div>';\n" +
                        "                                   data += '<div class=\"finish-stats\"> Cutoff: " + race.raceCutoffProperty().getValueSafe() + "</div>';\n" +
                        "				} else {\n" +
                        "                                   data += '<div class=\"finish-time\">' + rData.finish_display + '</div>';\n";
            if (showGun) report += "                                   data += '<div class=\"finish-stats\">Gun Time: ' + rData.gun_display + '</div>';\n"; 
            report +=   "                                   data += '<div class=\"finish-stats\">Overall: ' + rData.oa_place + '   Sex: ' + rData.sex_place + '   AG: ' + rData.ag_place + '</div>';\n" +
                        "                                   data += '<div class=\"finish-stats\">Pace: ' + rData.finish_pace + '</div>';\n" +
                        "				}" +
                        "				data += '</div>'; // time\n" +
                        "				\n" +
                        "                data += '</div>'; // row\n" +
                        "				\n";
//        if (showSplits) {
//            for (int i = 2; i < race.splitsProperty().size(); i++) {
//                report += "      <th data-priority=\"100\">" + race.splitsProperty().get(i-1).getSplitName() + "</th>" +  System.lineSeparator();
//            }
//        }
                        if (showSegments) {
                            final StringBuilder chars = new StringBuilder();
                            chars.append("data += '<div class=\"row\">';\n");
                            race.getSegments().forEach(seg -> {
                                chars.append("data += '<div class=\"segment\">'; // time\n");
                                chars.append("data += '<div class=\"segment-head\">" + seg.getSegmentName()+ "</div>';\n" );
                                chars.append("data += '<div class=\"segment-time\"Time: >' + rData.segments[\"segment_"+seg.getSegmentName()+ "\"].display + '</div>';\n");
                                if (showSegmentPace) chars.append("data += '<div class=\"segment-stats\">Pace:  ' + rData.segments[\"segment_"+seg.getSegmentName()+ "\"].pace + '</div>';\n");
                                chars.append("data += '<div class=\"segment-stats\">Overall: ' + rData.segments[\"segment_"+seg.getSegmentName()+ "\"].oa_place + '   Sex: ' + rData.segments[\"segment_"+seg.getSegmentName()+ "\"].sex_place + '   AG: ' + rData.segments[\"segment_"+seg.getSegmentName()+ "\"].ag_place + '</div>';\n");
                                chars.append("data += '</div>';\n");
                            });
                            chars.append("data += '</div>';\n");
                            report += chars.toString();
                        }
                        if (showSplits) {
                            report += "data += '<div class=\"row\">'; \n";
                            report += "data += '<div class=\"split\">'; \n";
                            report += "data += '<div class=\"split-head\">Splits:</div>';\n" ;
                            report += "data += '<table class=\"split-time\">' ;\n" ;
                            report += "data += '<tr><td>Start:</td><td>  ' + rData.start_display + '</div>';\n" ;
                            for (int i = 2; i < race.splitsProperty().size(); i++) {
                                report += "data += '<tr><td>" + race.splitsProperty().get(i-1).getSplitName() + ":</td><td class=\"right\">  ' + rData.splits[\"split_"+ race.splitsProperty().get(i-1).getSplitName() + "\"].display + '</td></tr>';\n";
                            }
                            report += "data += '<tr><td>Finish:</td><td>  ' + rData.finish_display + '</td></tr>';\n";
                            if (showGun) report += "data += '<tr><td>Gun Time:</td><td> ' + rData.gun_display + '</td></tr>';\n";
                            report += "data += '</table>';\n";
                            report += "data += '</div>';\n";
                            report += "data += '</div>';\n";

                        }
            report +=   "				\n" +
                        "				data += '</div>'; // detail\n" +
                        "				\n" +
                        "				return data;\n" +
                        "				}," +
                        "                type: 'column',\n" +
                        "                target: 'tr'\n" +
                        "            }\n" +
                        "        },\n" +
                        "    columnDefs: [ " +
                        "            { type: 'natural', targets: '_all' },\n" +
                        "            {className: 'control', orderable: false, targets:   0}\n" +
                        "        ],"  +
                        "    scrollY: '60vh',\n" +
                        "    scroller:    true,\n" +
                        "    deferRender: true,\n" +
                        "   \"fnInitComplete\": function () {\n" +
                        "	this.fnAdjustColumnSizing();\n" +
                        "	$('div.dataTables_filter input').focus();\n" +
                        "   }\n" +
                        "});\n" +
                    "   setTimeout( function () {\n" +
                    "		if (search !== \"\") oTable.rows( {search:'applied'} ).every(function(index){\n" +
                    "			var row = oTable.row(index);\n" +
                    "			row.node().click();\n" +
                    "		});\n" +
                    "		\n" +
                    "	}, 300 );" +
                    "} );\n" +
                    "\n" +
                    "\n" +
                    "	</script>\n";
        report += "<!-- End DataTables -->\n";
        
        report += "  </HEAD> " +  System.lineSeparator();
        report += "  <BODY> " +  System.lineSeparator();
        
        if (customHeaders){
            if (textOnlyHeaders) report += race.getStringAttribute("textHeader");
            else report += race.getStringAttribute("htmlHeader");
            report += System.lineSeparator();
        }
        
        report += "    <div class=\"event-info\">" + event.getEventName() + "<br>" + System.lineSeparator();;
        if (RaceDAO.getInstance().listRaces().size() > 1) 
            report += race.getRaceName() + "<br>" + System.lineSeparator();
        
        report += "    " + event.getLocalEventDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) + "</div>" + System.lineSeparator();
        report += System.lineSeparator();
        
        if(inProgress) {
            report += "    <div class=\"in-progress\">" + "*In Progress*" + "</div>" + System.lineSeparator();
            report += System.lineSeparator();
        }
        
        if (customHeaders){
            if (textOnlyHeaders) report += race.getStringAttribute("textMessage");
            else report += race.getStringAttribute("htmlMessage");
            report += System.lineSeparator();
        }
        
// Start the table
        report += "  <TABLE id=\"results\" class=\"display responsive nowrap\" > " +  System.lineSeparator();
        // print the headder
        report += "    <thead><tr>" +  System.lineSeparator();
        report += "      <th></th>"+  System.lineSeparator(); // dummy for control box
        report += "      <th data-priority=\"10\">OA#</th>" +  System.lineSeparator();
        report += "      <th data-priority=\"20\">SEX#</th>" +  System.lineSeparator();
        report += "      <th data-priority=\"30\">AG#</th>" +  System.lineSeparator();
        report += "      <th data-priority=\"5\">BIB</th>" +  System.lineSeparator(); 
        report += "      <th data-priority=\"9\">AGE</th>" +  System.lineSeparator(); 
        report += "      <th data-priority=\"5\">SEX</th>" +  System.lineSeparator(); 
        report += "      <th data-priority=\"29\">AG</th>" +  System.lineSeparator(); 
        report += "      <th data-priority=\"1\">Name</th>" +  System.lineSeparator(); 
        report += "      <th data-priority=\"41\">City</th>" +  System.lineSeparator(); 
        report += "      <th data-priority=\"40\">ST</th>" +  System.lineSeparator(); 
        report += "      <th data-priority=\"45\">Country</th>" +  System.lineSeparator(); 
 
        // Insert split stuff here
        if (showSplits) {
            for (int i = 2; i < race.splitsProperty().size(); i++) {
                report += "      <th data-priority=\"100\">" + race.splitsProperty().get(i-1).getSplitName() + "</th>" +  System.lineSeparator();
            }
        }
        if (showSegments) {
            final StringBuilder chars = new StringBuilder();
            Integer dispLeg = dispFormatLength;
            race.getSegments().forEach(seg -> {
                chars.append("      <th data-priority=\"80\">" + seg.getSegmentName()+ "</th>" +  System.lineSeparator());
                if (showSegmentPace) chars.append("      <th data-priority=\"95\"> Pace</th>" +  System.lineSeparator()); // pace.getFieldWidth()+1
            });
            report += chars.toString();
        }
        // Chip time
        report += "      <th data-priority=\"1\">Finish</th>" +  System.lineSeparator(); // 9R Need to adjust for the format code
       
        // gun time
        if (showGun) report += "      <th data-priority=\"90\">Gun</th>" +  System.lineSeparator(); // 9R ibid
        // pace
        if (showPace) report += "      <th data-priority=\"85\">Pace</th>" +  System.lineSeparator(); // 10R
        
        report += "</tr></thead>" +  System.lineSeparator(); 
        
        
        
        final StringBuilder chars = new StringBuilder();
        
        
        report += "</table>" +  System.lineSeparator();
        if (customHeaders){
            if (textOnlyHeaders) report += race.getStringAttribute("textFooter");
            else report += race.getStringAttribute("htmlFooter");
            report += System.lineSeparator();
        }
        report += "  </BODY> " +  System.lineSeparator();
        report += "</HTML> " +  System.lineSeparator();
        
        return report;
    }
    
    
}