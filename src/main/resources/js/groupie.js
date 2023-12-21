(function ($) {
    var url = AJS.contextPath() + "/rest/groupie/1.0/";

    // wait for the DOM
    $(function() {
        // request the config information from the server
        $.ajax({
            url: url,
            dataType: "json"
        }).done(function(content) {
            // populate the form
            $("#mapping").val(JSON.stringify(content, null, 2));
        });

        function updateConfig() {
                    $.ajax({
                        url: url,
                        type: "PUT",
                        contentType: "application/json",
                        data: $("#mapping").val(),
                        processData: false
                    }).done(function(){
                        alert("✅ Changes were successfully saved!");
                    }).fail(function(jqXHR, textStatus){
                        alert(`❌ Invalid JSON, changes were not applied. (${textStatus}: ${jqXHR.status})`);
                    });
                }

        $("#admin").on("submit", function(e) {
            e.preventDefault();
            updateConfig();
        });
    });
})(AJS.$ || jQuery);