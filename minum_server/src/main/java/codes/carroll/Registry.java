package codes.carroll;

import com.renomad.minum.web.RequestLine;
import com.renomad.minum.Context;
import com.renomad.minum.database.Db;
import com.renomad.minum.templating.TemplateProcessor;
import com.renomad.minum.web.Request;
import com.renomad.minum.web.Response;
import com.renomad.minum.web.WebFramework;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.List;

public class Registry {

        private final Context context;
        private final WebFramework webFramework;
        private final TemplateProcessor yesterdayTemplateProcessor;
        private final TemplateProcessor todayTemplateProcessor;
        private final TemplateProcessor historyTemplateProcessor;

        public Registry(Context context) {
                String base_template = context
                                .getFileUtils()
                                .readTextFile("src/main/webapp/templates/base.html");
                String today_template = context
                                .getFileUtils()
                                .readTextFile("src/main/webapp/templates/today.html");
                String yesterday_template = context
                                .getFileUtils()
                                .readTextFile("src/main/webapp/templates/yesterday.html");
                String history_template = context
                                .getFileUtils()
                                .readTextFile("src/main/webapp/templates/history.html");

                TemplateProcessor baseTemplateProcessor = TemplateProcessor.buildProcessor(base_template);

                this.todayTemplateProcessor = TemplateProcessor.buildProcessor(
                                baseTemplateProcessor.renderTemplate(
                                                Map.of("which", "Todaydle", "content", today_template)));

                this.yesterdayTemplateProcessor = TemplateProcessor.buildProcessor(
                                baseTemplateProcessor.renderTemplate(
                                                Map.of("which", "Yesterdle", "content", yesterday_template)));

                this.historyTemplateProcessor = TemplateProcessor.buildProcessor(
                                baseTemplateProcessor.renderTemplate(
                                                Map.of("which", "Histordle", "content", history_template)));

                this.context = context;
                this.webFramework = context.getFullSystem().getWebFramework();
        }

        public void registerDomains() {
                webFramework.registerPath(RequestLine.Method.GET, "", r -> Response.redirectTo("yesterday"));
                webFramework.registerPath(RequestLine.Method.GET, "yesterday", this::yesterday);
                webFramework.registerPath(RequestLine.Method.GET, "today", this::today);
                webFramework.registerPath(RequestLine.Method.GET, "history", this::history);
        }

        public Response history(Request r) {
                Db<WordModel> words = context.getDb("words", WordModel.EMPTY);
                String worldListHtml = "";

                for (WordModel word : words.values()
                                .stream()
                                .toList()
                                .subList(0, words.values().size() - 1)
                                .reversed()) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E MM/dd/yyyy");
                        String formattedString = word.timestamp.format(formatter);
                        worldListHtml += "<li> <a id='word'>" + word.word + "</a></br><a id='date'>" + formattedString
                                        + "</a></li>";
                }

                Map<String, String> templateValues = Map.of(
                                "wordlist", worldListHtml);
                return Response.htmlOk(historyTemplateProcessor.renderTemplate(templateValues));
        }

        public Response today(Request r) {
                Optional<WordModel> word = Optional.empty();

                try {
                        Db<WordModel> words = context.getDb("words", WordModel.EMPTY);
                        word = Optional.of(words.values().stream().toList().getLast());
                } catch (Exception e) {
                }

                String wordText = word.get().word;
                ZonedDateTime wordDateTime = word.get().timestamp;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                String formattedString = wordDateTime.format(formatter);

                Map<String, String> templateValues = Map.of(
                                "word", wordText,
                                "date", formattedString);
                return Response.htmlOk(todayTemplateProcessor.renderTemplate(templateValues));
        }

        public Response yesterday(Request r) {
                Optional<WordModel> word = Optional.empty();

                try {
                        Db<WordModel> words = context.getDb("words", WordModel.EMPTY);
                        List<WordModel> wordList = words.values().stream().toList();
                        word = Optional.of(wordList.get(wordList.size() - 2));
                } catch (Exception e) {
                }

                String wordText = word.get().word;
                ZonedDateTime wordDateTime = word.get().timestamp;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                String formattedString = wordDateTime.format(formatter);

                Map<String, String> templateValues = Map.of(
                                "word", wordText,
                                "date", formattedString);
                return Response.htmlOk(yesterdayTemplateProcessor.renderTemplate(templateValues));
        }
}