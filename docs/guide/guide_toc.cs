<?cs # Table of contents for Dev Guide.

       For each document available in translation, add an localized title to this TOC.
       Do not add localized title for docs not available in translation.
       Below are template spans for adding localized doc titles. Please ensure that
       localized titles are added in the language order specified below.
?>
<ul id="nav">
  <!--  Walkthrough for Developers -- quick overview of what it's like to develop on Android -->
  <!--<li style="color:red">Overview</li> -->

  <li class="nav-section">
    <div class="nav-section-header"><a href="<?cs var:toroot ?>guide/components/index.html">
        <span class="en">App Components</span>
      </a></div>
    <ul>
      <li><a href="<?cs var:toroot ?>guide/components/fundamentals.html">
            <span class="en">App Fundamentals</span></a>
      </li>
      <li class="nav-section">
        <div class="nav-section-header"><a href="<?cs var:toroot ?>guide/components/services.html">
            <span class="en">Services</span>
          </a></div>
        <ul>
          <li><a href="<?cs var:toroot ?>guide/components/bound-services.html">
              <span class="en">Bound Services</span>
            </a></li>
        </ul>
      </li>
      <li><a href="<?cs var:toroot ?>guide/components/intents.html">
          <span class="en">Intents</span>
        </a>
      </li>
      <li><a href="<?cs var:toroot ?>guide/components/processes-and-threads.html">
          <span class="en">Processes and Threads</span>
        </a>
      </li>
      <li class="nav-section">
        <div class="nav-section-header"><a href="<?cs var:toroot ?>guide/topics/manifest/manifest-intro.html">
          <span class="en">Mindroid Manifest</span>
        </a></div>
        <ul>
          <li><a href="<?cs var:toroot ?>guide/topics/manifest/application-element.html">&lt;application&gt;</a></li>
          <li><a href="<?cs var:toroot ?>guide/topics/manifest/manifest-element.html">&lt;manifest&gt;</a></li>
          <li><a href="<?cs var:toroot ?>guide/topics/manifest/service-element.html">&lt;service&gt;</a></li>
		  <li><a href="<?cs var:toroot ?>guide/topics/manifest/uses-library-element.html">&lt;uses-library&gt;</a></li>
        </ul>
      </li>
    </ul>
  </li>

  <li class="nav-section">
      <div class="nav-section-header"><a href="<?cs var:toroot ?>guide/topics/data/index.html">
          <span class="en">Data Storage</span>
        </a></div>
      <ul>
         <li><a href="<?cs var:toroot ?>guide/topics/data/data-storage.html">
            <span class="en">Storage Options</span>
           </a></li>
      </ul>
  </li>  
</ul>


<script type="text/javascript">
<!--
    buildToggleLists();
    changeNavLang(getLangPref());
//-->
</script>

