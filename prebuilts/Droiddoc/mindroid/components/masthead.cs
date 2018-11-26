<?cs def:custom_masthead() ?>
    <!-- Header -->
    <div id="header">
        <div class="wrap" id="header-wrap">
          <div class="col-3 logo">
          <a href="<?cs var:toroot ?>index.html">
            <img src="<?cs var:toroot ?>assets/images/dac_logo.png" width="123" height="25" alt="Mindroid Developers" />
          </a>
          <div class="btn-quicknav" id="btn-quicknav">
          	<a href="#" class="arrow-inactive">Quicknav</a>
			      <a href="#" class="arrow-active">Quicknav</a>
          </div>
          </div>
            <ul class="nav-x col-9">
                <li class="develop last"><a href="<?cs var:toroot ?>develop/index.html" <?cs
                  if:training || guide || reference || tools || develop || google ?>class="selected"<?cs /if ?>
                  zh-TW-lang="開發"
                  zh-CN-lang="开发"
                  ru-lang="Разработка"
                  ko-lang="개발"
                  ja-lang="開発"
                  es-lang="Desarrollar"               
                  >Develop</a></li>
            </ul>
            
            <!-- New Search -->
            <div class="menu-container">
            <div class="moremenu">
    <div id="more-btn"></div>
  </div>
  <div class="morehover" id="moremenu">
    <div class="top"></div>
    <div class="mid">
      <div class="header">Links</div>
      <ul>
        <li><a href="http://esrlabs.com/">ESR Labs</a></li>
      </ul>
      <br class="clearfix" />
    </div>
    <div class="bottom"></div>
  </div>
  <div class="search" id="search-container">
    <div class="search-inner">
      <div id="search-btn"></div>
      <div class="left"></div>
      <form onsubmit="return submit_search()">
        <input id="search_autocomplete" type="text" value="" autocomplete="off" name="q"
onfocus="search_focus_changed(this, true)" onblur="search_focus_changed(this, false)"
onkeydown="return search_changed(event, true, '<?cs var:toroot ?>')" 
onkeyup="return search_changed(event, false, '<?cs var:toroot ?>')" />
      </form>
      <div class="right"></div>
        <a class="close hide">close</a>
        <div class="left"></div>
        <div class="right"></div>
    </div>
  </div>
  <div id="search_filtered_wrapper">
    <div id="search_filtered_div" class="no-display">
        <ul id="search_filtered">
        </ul>
    </div>
  </div>
  
  </div>
  <!-- /New Search>
          
          
          <!-- Expanded quicknav -->
           <div id="quicknav" class="col-9">
                <ul>
                    <li class="develop last">
                      <ul>
                        <li><a href="<?cs var:toroot ?>guide/components/index.html">Guides</a>
                        </li>
                        <li><a href="<?cs var:toroot ?>reference/packages.html">Reference</a>
                        </li>
                      </ul>
                    </li>
                </ul>
          </div>
          <!-- /Expanded quicknav -->
        </div>
    </div>
    <!-- /Header -->
    
    
  <div id="searchResults" class="wrap" style="display:none;">
          <h2 id="searchTitle">Results</h2>
          <div id="leftSearchControl" class="search-control">Loading...</div>
  </div>
    
    
    
<?cs if:training || guide || reference || tools || develop || google ?>
    <!-- Secondary x-nav -->
    <div id="nav-x">
        <div class="wrap">
            <ul class="nav-x col-9 develop" style="width:100%">
                <li><a href="<?cs var:toroot ?>guide/components/index.html" <?cs
                  if:guide ?>class="selected"<?cs /if ?>
                  >Guides</a></li>
                <li><a href="<?cs var:toroot ?>reference/packages.html" <?cs
                  if:reference && !(reference.gcm || reference.gms) ?>class="selected"<?cs /if ?>
                  >Reference</a></li>
            </ul>
        </div>
        
    </div>
    <!-- /Sendondary x-nav -->
<?cs /if ?>
  <?cs 
/def ?>
