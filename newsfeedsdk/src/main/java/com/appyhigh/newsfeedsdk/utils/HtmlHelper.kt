package com.appyhigh.newsfeedsdk.utils

object HtmlHelper {
    fun getSmallScreenHtml(coinSymbol: String,interval: String, theme:String): String{
        return "<html style = \"background-color: #131722\">" +
                "<body style=\"margin: 0;\">" +
                "<!-- TradingView Widget BEGIN -->\n" +
                "<div class=\"tradingview-widget-container\">\n" +
                "    <div id=\"tradingview_368be\"></div>\n" +
                "  <script type=\"text/javascript\" src=\"https://s3.tradingview.com/tv.js\"></script>\n" +
                "  <script type=\"text/javascript\">\n" +
                "  new TradingView.widget(\n" +
                "  {\n" +
                "  \"autosize\": true,\n" +
                "  \"symbol\": \"$coinSymbol\",\n" +
                "  \"interval\": \"$interval\",\n" +
                "  \"timezone\": \"Asia/Kolkata\",\n" +
                "  \"theme\": \"$theme\",\n" +
                "  \"style\": \"1\",\n" +
                "  \"locale\": \"in\",\n" +
                "  \"toolbar_bg\": \"#f1f3f6\",\n" +
                "  \"enable_publishing\": false,\n" +
                "  \"hide_top_toolbar\": true,\n" +
                "  \"hide_side_toolbar\": false,\n" +
                "  \"hide_legend\": true,\n" +
                "  \"container_id\": \"tradingview_5eea6\"\n" +
                "}\n" +
                "  );\n" +
                "  </script>" +
                "</div>\n" +
                "<!-- TradingView Widget END -->\n" +
                "</body>\n" +
                "</html>"
    }

    fun getFullScreenHtml(coinSymbol: String): String{
        return "<html style = \"background-color: #131722\">" +
                "<body style=\"margin: 0;\">" +
                "<!-- TradingView Widget BEGIN -->\n" +
                "<div class=\"tradingview-widget-container\">\n" +
                "    <div id=\"tradingview_368be\"></div>\n" +
                "    <script type=\"text/javascript\" src=\"https://s3.tradingview.com/tv.js\"></script>\n" +
                "    <script type=\"text/javascript\">\n" +
                "  new TradingView.widget(\n" +
                "  {\n" +
                "  \"autosize\": true,\n" +
                "  \"symbol\": \"$coinSymbol\",\n" +
                "  \"interval\": \"D\",\n" +
                "  \"timezone\": \"Asia/Kolkata\",\n" +
                "  \"theme\": \"dark\",\n" +
                "  \"style\": \"1\",\n" +
                "  \"locale\": \"in\",\n" +
                "  \"toolbar_bg\": \"#f1f3f6\",\n" +
                "  \"enable_publishing\": false,\n" +
                "  \"hide_top_toolbar\": true,\n" +
                "  \"withdateranges\": true,\n" +
                "  \"hide_side_toolbar\": false,\n" +
                "  \"container_id\": \"tradingview_368be\"\n" +
                "}\n" +
                "  );\n" +
                "  </script>\n" +
                "</div>\n" +
                "<!-- TradingView Widget END -->\n" +
                "</body>\n" +
                "</html>"
    }
}