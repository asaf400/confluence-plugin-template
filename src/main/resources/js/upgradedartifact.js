(function ($) { // this closure helps us keep our variables to ourselves.
// This pattern is known as an "iife" - immediately invoked function expression

    // form the URL
    var url = AJS.contextPath() + "/rest/upgraded/1.0/";

    // wait for the DOM (i.e., document "skeleton") to load.
    $(function() {
        // request the config information from the server
        $.ajax({
            url: url,
            dataType: "json"
        }).done(function(content) { // when the configuration is returned...
            // ...populate the form.
            $("#mapping").val(JSON.stringify(content));
        });

        function updateConfig() {
                    $.ajax({
                        url: url,
                        type: "PUT",
                        contentType: "application/json",
                        data: $("#mapping").val(),
                        processData: false
                    });
                }

        $("#admin").on("submit", function(e) {
            e.preventDefault();
            updateConfig();
        });
    });
})(AJS.$ || jQuery);