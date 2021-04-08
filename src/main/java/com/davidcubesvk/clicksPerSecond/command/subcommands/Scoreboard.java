package com.davidcubesvk.clicksPerSecond.command.subcommands;

import com.davidcubesvk.clicksPerSecond.ClicksPerSecond;
import com.davidcubesvk.clicksPerSecond.api.ClicksPerSecondAPI;
import com.davidcubesvk.clicksPerSecond.api.ScoreboardType;
import com.davidcubesvk.clicksPerSecond.command.CommandProcessor;
import com.davidcubesvk.clicksPerSecond.test.TestRecord;
import com.davidcubesvk.clicksPerSecond.utils.command.CommandUtil;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Handler for scoreboard sub-command.
 */
public class Scoreboard implements CommandProcessor {

    @Override
    public void onCommand(CommandSender sender, String[] args, CommandUtil commandUtil) {
        //Check the permission and args
        if (!commandUtil.hasPermission("cps.command.scoreboard") || !commandUtil.checkArgs(2, 3) || commandUtil.isFormatOutdated())
            return;

        //Parse the ScoreboardType
        ScoreboardType scoreboardType;
        try {
            //Parse
            scoreboardType = ScoreboardType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException ex) {
            //Invalid format
            commandUtil.invalidFormat();
            return;
        }

        //Get scoreboard
        List<TestRecord> list = ClicksPerSecondAPI.getInstance().getCachedScoreboard(scoreboardType);
        //Check if no statistics to display
        if (list.size() == 0) {
            commandUtil.sendMessage("command.main.scoreboard.noStatistics");
            return;
        }

        //How many places to display per page
        int perPage = ClicksPerSecond.getConfiguration().getInt("command.main.scoreboard.perPage");
        //Get the amount of pages available to display
        int pageValue = getPages(list, perPage);
        //Check the page index
        int page = checkPageNumber(pageValue, args, commandUtil);

        //If invalid page index, return
        if (page == -1)
            return;

        //Header
        commandUtil.sendMessage("command.main.scoreboard.message." + scoreboardType.getName() + ".top", message -> message.replace("{page}", "" + page).replace("{pages}", "" + pageValue));

        for (int index = 0; index < perPage; index++) {
            int pos = (page - 1) * perPage + index;

            //Break if count is out of page size
            if (pos >= list.size()) {
                break;
            }

            //Get the TestRecord
            TestRecord testRecord = list.get(pos);

            //Send line
            commandUtil.sendMessage("command.main.scoreboard.message." + scoreboardType.getName() + ".line", testRecord::setPlaceholders);
        }

        //Footer
        commandUtil.sendMessage("command.main.scoreboard.message." + scoreboardType.getName() + ".bottom", message -> message.replace("{page}", "" + page).replace("{pages}", "" + pageValue));
    }

    /**
     * Returns the page index from the command argument, or default index (if not specified) and checks:
     * <ol>
     * <li>If page argument is specified and if it is a number</li>
     * <li>If it is bigger than 0</li>
     * <li>If the page index is out of range of available pages to display</li>
     * </ol>
     * If any of these rules are violated, sends a message to sender corresponding to the violated rule and returns <code>-1</code> representing an invalid page index.
     *
     * @param pages       the amount of pages available to be displayed
     * @param args        command arguments
     * @param commandUtil command util instance used to send messages
     * @return the page index to display, or -1 if invalid
     */
    private int checkPageNumber(int pages, String[] args, CommandUtil commandUtil) {
        //Page to display
        int page;
        try {
            //Default value or parse from arguments
            page = args.length == 2 ? 1 : Integer.parseInt(args[2]);

            //If index is smaller than 1
            if (page < 1)
                throw new Exception();
        } catch (Exception ex) {
            //Invalid number
            commandUtil.sendMessage("command.main.scoreboard.pageNumber");
            return -1;
        }

        //If the page index is out of range of available pages
        if (page > pages) {
            commandUtil.sendMessage("command.main.scoreboard.pageBigger", message -> message.replace("{page}", "" + page).replace("{pages}", "" + pages));
            return -1;
        }

        return page;
    }

    /**
     * Splits the given scoreboard into pages (depending on the perPage parameter) and returns the amount of pages.
     *
     * @param scoreboard the scoreboard data
     * @param perPage    the amount of scoreboard places to display per page
     * @return the amount of pages that can be displayed
     */
    private int getPages(List<TestRecord> scoreboard, int perPage) {
        //Check if list is empty
        if (scoreboard.size() == 0)
            return 0;

        //Value of pages just divided by per page parameter
        int pageValue = scoreboard.size() / perPage;
        //If the amount of scoreboard places is not a multiply of per page parameter (for example 2*perPage < amount < 3*perPage)
        if (scoreboard.size() % perPage > 0.0)
            pageValue++;

        return pageValue;
    }

}
