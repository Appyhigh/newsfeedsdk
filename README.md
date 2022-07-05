# NewsFeedSDK

A news feed sdk to seamlessly integrate news in your application

# Table of contents

- [Initialization](#initialization)
- [Custom Views](#custom-views)
- [Features](#features)
- [Handle Notifications & Dynamic Links](#handle-notifications--dynamic-links)
- [Notification Payload Examples](#notification-payload-examples)

## Initialization
[(Back to top)](#table-of-contents)

1)  Import and add the module to your app `build.gradle` file and `settings.gradle`.

    In your  `build.gradle`:

    ```groovy

        dependencies {
            implementation project(path: ':newsfeedsdk')
            // ...
        }
    ```
2)  Add `minifyEnabled` as **false** inside `buildTypes` in your app `build.gradle` file.
    ```groovy
        buildTypes {
                debug {
                    minifyEnabled false
                    // ...
                }
                release {
                    minifyEnabled false
                    // ...
                }
            }
    ```
3)  Add the following metadata inside your `application` tag in your `AndroidManifest.xml`

    ```xml
    <application>
        <meta-data
            android:name="app_name"
            android:value="**parent app name**" />
        <meta-data
            android:name="news_feed_app_id"
            android:value="**feedsdk app_id for app**" />
        <meta-data
            android:name="feed_target_activity"
            android:value="**activity that has feeds integrated**"/>
        <meta-data
            android:name="feed_app_icon"
            android:resource="**icon of parent app**"/>
        <meta-data
            android:name="is_location_already_asked"
            android:value="**if(location is already being asked in parent app) true else false" />
    </application>
    ```
    #### Example
    ```xml
        <meta-data
            android:name="app_name"
            android:value="@string/app_name" />
        <meta-data
            android:name="news_feed_app_id"
            android:value="@string/news_feed_app_id" />
        <meta-data
            android:name="feed_target_activity"
            android:value="com.appyhigh.exampleapp.activity.HomeActivity"/>
        <meta-data
            android:name="feed_app_icon"
            android:resource="@drawable/ic_app_logo"/>
        <meta-data
            android:name="is_location_already_asked"
            android:value="false" />
    ```
4)  Inititalize SDK(Recommended to initialize in Splash or Main Activity)

    initializeSdk(context: Context, lifecycle: Lifecycle, user: User? = null)

    ***Kotlin***
    ```kotlin
    private fun initFeedSDK() {
         FeedSdk().initializeSdk(this, lifecycle, user) //user can be null also
         FeedSdk().setFirebaseDynamicLink(**dynamic link domain of parent app**)
         FeedSdk().setShareBody(** shareBody text of the dynamic links of posts that are shared from feedsdk **)
         SpUtil.getGEOPoints(this)  // this is required for feedSDK to set the language by location
    }
    ```
    ***Java***
    ```java
    void initFeedSDK(){
         new FeedSdk().initializeSdk(this, getLifecycle(), user); //user can be null also
         FeedSdk.Companion.setMFirebaseDynamicLink(**dynamic link domain of parent app**)
         FeedSdk.Companion.setShareBody(** shareBody text of the dynamic links of posts that are shared from feedsdk **)
         SpUtil.Companion.getGEOPoints(this)  // this is required for feedSDK to set the language by location
     }
    ```

5)  [**Handle Notifications & Dynamic Links**](#handle-notifications--dynamic-links)

## Custom Views
[(Back to top)](#table-of-contents)
- [News Feed](#news-feed)
- [Reels](#reels)
- [Explore](#explore)
- [Cricket Feed](#cricket-feed)

### News Feed
[(Back to top)](#custom-views)

<img src="https://github.com/tejaSomanchi/feedDoc/blob/main/20210928_182405.gif" width="300" />

1)  Add the following lines inside your xml to show the News Feed view
    ```xml
    <com.appyhigh.newsfeedsdk.customview.NewsFeedList
       android:id="@+id/newsFeed"
       android:layout_width="match_parent"
       android:layout_height="match_parent"
       />
    ```
2)  Create object of custom view and call the following functions according to your requirements.
    ```kotlin
    val newsFeedList = view.findViewById(R.id.newsFeed)     //id of NewsFeedList defined in xml
    newsFeedList.stopVideoPlayback()    // to stop playing videos in this view
    newsFeedList.startVideoPlayback()   // to start playing videos in this view
    newsFeedList.refreshFeeds()     // to refresh feeds in this view
    ``` 

### Reels
[(Back to top)](#custom-views)

<img src="https://github.com/tejaSomanchi/feedDoc/blob/main/20210928_183834.gif" width="300" />

1)  Add the following lines inside your xml to show the Reels view
    ```xml
    <com.appyhigh.newsfeedsdk.customview.VideoFeed
        android:id="@+id/video_feed"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>
    ```
2)  Create object of custom view and call the following functions according to your requirements.
    ```kotlin
    val videoFeed = view.findViewById(R.id.video_feed)   //id of VideoFeed defined in xml
    videoFeed.onFocusChanged()  // to stop playing videos in this view
    videoFeed.onResume()    // to start playing videos in this view
    ``` 

### Explore
[(Back to top)](#custom-views)

<img src="https://github.com/tejaSomanchi/feedDoc/blob/main/20210928_184155.gif" width="300" />

1)  Add the following lines inside your xml to show the Explore view
    ```xml
    <com.appyhigh.newsfeedsdk.customview.ExploreFeed
        android:id="@+id/explore_feed"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>
    ```

### Cricket Feed
[(Back to top)](#custom-views)

<img src="https://github.com/tejaSomanchi/feedDoc/blob/main/20210928_184633.gif" width="300" />

1)  Add the following lines inside your xml to show the Cricket Feed view
    ```xml
    <com.appyhigh.newsfeedsdk.customview.CricketView
       android:id="@+id/cricketFeed"
       android:layout_width="match_parent"
       android:layout_height="match_parent"/>
    ```
2)  Create object of custom view and call the following functions according to your requirements.
    ```kotlin
    val cricketFeed = view.findViewById(R.id.cricketFeed)     //id of CricketView defined in xml
    cricketFeed.stopVideoPlayback()    // to stop playing videos in this view
    cricketFeed.refreshFeeds()     // to refresh feeds in this view
    ``` 

## Features
[(Back to top)](#table-of-contents)

- [Ads](#ads)
- [Search Sticky Bar](#search-sticky-bar)
- [Bottom Navigation Nudge](#bottom-navigation-nudge)

### Ads
[(Back to top)](#features)

1) Add your ad units after initializing the sdk to show the ads.

   ***Kotlin***
    ```kotlin
    private fun initFeedSDK() {
         FeedSdk().initializeSdk(this, lifecycle, user) //user can be null also
         // ...
         // all the ad ids should be native ad units except search_footer_banner_intermediate, native_footer_banner, ad_id_post_interstitial
         val adsModel = AdsModel(
            getString(R.string.ad_unit_feed_native),
            getString(R.string.ad_unit_video_ad_native),
            getString(R.string.ad_unit_team_ranking),
            getString(R.string.ad_unit_player_ranking),
            getString(R.string.ad_unit_live_score),
            getString(R.string.ad_unit_scorecard),
            getString(R.string.ad_unit_commentary),
            getString(R.string.ad_unit_finished_match),
            getString(R.string.search_footer_banner_intermediate)
        )
        FeedSdk().setAdsModel(adsModel)
        // ...
    }
    ```
   ***Java***
    ```java
    void initFeedSDK(){
         new FeedSdk().initializeSdk(this, getLifecycle(), user); //user can be null also
         // all the ad ids should be native ad units except search_footer_banner_intermediate, native_footer_banner, ad_id_post_interstitial
         // ...
          AdsModel adsModel = new AdsModel(
                getString(R.string.ad_unit_feed_native),
                getString(R.string.ad_unit_video_ad_native),
                getString(R.string.ad_unit_team_ranking),
                getString(R.string.ad_unit_player_ranking),
                getString(R.string.ad_unit_live_score),
                getString(R.string.ad_unit_scorecard),
                getString(R.string.ad_unit_commentary),
                getString(R.string.ad_unit_finished_match),
                getString(R.string.search_footer_banner)
        );        
        FeedSdk.Companion.setMAdsModel(adsModel);
        // ...
     }
    ```
2) Add the following line after initializing the sdk to hide the ads in the sdk.

   ***Kotlin***
   ```kotlin
   private fun initFeedSDK() {
        FeedSdk().initializeSdk(this, lifecycle, user) //user can be null also
        // ...
        FeedSdk().setShowAds(false)
       // ...
   }
   ```
   ***Java***
   ```java
   void initFeedSDK(){
        new FeedSdk().initializeSdk(this, getLifecycle(), user); //user can be null also  
        // ...
       FeedSdk.Companion.setShowAds(false);
       // ...
    }
   ```
   **Note : By Default, ads are shown in Feedsdk**

### Search Sticky Bar
[(Back to top)](#features)

<img src="https://github.com/tejaSomanchi/feedDoc/blob/main/search_sticky_bar.jpg" width="600" />

1)  Add the following line after initializing the sdk to show the search sticky bar in the notification tray.

    ***Kotlin***
    ```kotlin
    private fun initFeedSDK() {
         FeedSdk().initializeSdk(this, lifecycle, user) //user can be null also
         // ...
         FeedSdk().setSearchStickyNotification(defaultBackground:String, intent: Intent)
        // ...
    }
    ```
    ***Java***
    ```java
    void initFeedSDK(){
         new FeedSdk().initializeSdk(this, getLifecycle(), user); //user can be null also  
         // ...
        new FeedSdk().setSearchStickyNotification(defaultBackground, getIntent());
        // ...
     }
    ```
2) Add the following if condition before initializing the sdk.

   ***Kotlin***
   ```kotlin
       if(intent.hasExtra("fromSticky") && intent.getStringExtra("fromSticky")=="reels"){
            com.appyhigh.newsfeedsdk.Constants.isVideoFromSticky = true
        }
        // ...
       FeedSdk().initializeSdk(this, lifecycle, user) //user can be null also
        // ...
       
   ```
   ***Java***
   ```java
       if(getIntent().hasExtra("fromSticky") && getIntent().getStringExtra("fromSticky").equals("reels")){
           com.appyhigh.newsfeedsdk.Constants.INSTANCE.setVideoFromSticky(true);
       }
       // ...
       new FeedSdk().initializeSdk(this, getLifecycle(), user); //user can be null also  
       // ...
   ``` 

3) Add the following lines after initializing the sdk where you handle your parent app notification intent.

***Kotlin***
   ```kotlin
       if(intent.hasExtra("fromSticky")){
            Handler(Looper.getMainLooper()).postDelayed({
                if(intent.getStringExtra("fromSticky")=="reels") {
                   //open tab or activity that handles reels view of feedSDK
		   //ignore if not using reels view in parent app
		} else{
		   //open tab or activity that handles feeds view of feedSDK
		   //ignore if not using feeds view in parent app
		}
            },1000)
        }
       // ...
   ```
***Java***
   ```java
       if(intent.hasExtra("fromSticky")){
		new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
		    @Override
		    public void run() {
			if(intent.getStringExtra("fromSticky").equals("reels")) {
			   //open tab or activity that handles reels view of feedSDK
                  	   //ignore if not using reels view in parent app
			} else{
			   //open tab or activity that handles feeds view of feedSDK
                  	   //ignore if not using feeds view in parent app
			}
		    }
		}, 1000);
    	}
       // ...
   ```
4) Start `SettingsActivity` to open the settings for this notification in parent app.
   ***Kotlin***
   ```kotlin
    startActivity(Intent(this, SettingsActivity::class.java))
   ```
   ***Java***
   ```java
   startActivity(new Intent(this, SettingsActivity.class));
   ```


### Bottom Navigation Nudge
[(Back to top)](#features)

1) Pass the bottom navigation instance, the position of icon where you want to show nudge, number of notification (optional), Also save the result for removing the nudge
    
    ***Kotlin***
    ```kotlin
    val object = FeedSdk().setNudge(bottomNavigation,1,5)
   ```
   
    ***Java***
    ```java
    BottomNavigationItemView object = new FeedSdk().setNudge(bottomNavigation,1,5);
   ```
    
2) Whenever you want to remove the nudge, pass the object saved in the above step as follows:
    
    ***Kotlin***
    ```kotlin
    if(object != null)
        FeedSdk().removeNudge(object)
    ```
   
    ***Java***
    ```java
    if(object != null)
        new FeedSdk().removeNudge(object);
    ```


## Handle Notifications & Dynamic Links

[(Back to top)](#table-of-contents)

### Handle Notifications


1) Add the following if condition before initializing the sdk.

   ***Kotlin***
   ```kotlin
       if(FeedSdk.isScreenNotification(intent)){
           SpUtil.pushIntent = intent
       } else{
           SpUtil.pushIntent = null
       }
        // ...
       FeedSdk().initializeSdk(this, lifecycle, user) //user can be null also
        // ...
       
   ```
   ***Java***
   ```java
       if(FeedSdk.Companion.isScreenNotification(getIntent())){
           SpUtil.Companion.setPushIntent(getIntent());
       } else{
            SpUtil.Companion.setPushIntent(null);
        }
       // ...
       new FeedSdk().initializeSdk(this, getLifecycle(), user); //user can be null also  
       // ...
   ```
2) Add the following lines after initializing the sdk where you handle your parent app notification intent.

   ***Kotlin***
   ```kotlin
       if(FeedSdk.isScreenNotification(intent) || FeedSdk.fromLiveMatch(intent)){
           Handler(Looper.getMainLooper()).postDelayed({
               if(FeedSdk.checkFeedSdkTab("explore", intent)){
                  //open tab or activity that handles explore view of feedSDK
                  //ignore if not using explore view in parent app
               } else if(FeedSdk.checkFeedSdkTab("reels", intent)){
                   //open tab or activity that handles reels view of feedSDK
                   //ignore if not using reels view in parent app
               } else {
                  //open tab or activity that handles feeds view of feedSDK
                  //ignore if not using feeds view in parent app
               }
           },1000)
       } else if (intent.extras != null && !FeedSdk.handleIntent(this, intent)) {
            //Handle your parent app notifications
       }
       // ...
   ```
   ***Java***
   ```java
       if(FeedSdk.Companion.isScreenNotification(getIntent()) || FeedSdk.Companion.fromLiveMatch(getIntent())){
           new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
               @Override
               public void run() {
                   if(FeedSdk.Companion.checkFeedSdkTab("explore", getIntent())){
                       //open tab or activity that handles explore view of feedSDK
                       //ignore if not using explore view in parent app
                   } else if(FeedSdk.Companion.checkFeedSdkTab("reels", getIntent())){
                       //open tab or activity that handles reels view of feedSDK
                       //ignore if not using reels view in parent app
                   } else {
                      //open tab or activity that handles feeds view of feedSDK
                      //ignore if not using feeds view in parent app
                   }
               }
           }, 1000);
       } else if (getIntent().getExtras() != null && !FeedSdk.Companion.handleIntent(this, getIntent())) {
            //Handle your parent app notifications
       }
       // ...
   ```


### Handle Dynamic Links

[(Back to top)](#handle-notifications--dynamic-links)

1) Fetch and check the params("feed_id","podcast_id","filename","matchType","matchesMode") from firebase dynamic link.

   ***Kotlin***
   ```kotlin
      Firebase.dynamicLinks
           .getDynamicLink(intent)
           .addOnSuccessListener(this) { pendingDynamicLinkData ->
               // Get deep link from result (may be null if no link is found)
               var deepLink: Uri? = null
              try {
                  if (pendingDynamicLinkData != null) {
                      deepLink = pendingDynamicLinkData.link
                      try {
                          if (deepLink!!.getQueryParameter("feed_id") != null) {
                              post_id= pendingDynamicLinkData.link?.getQueryParameter("feed_id")!!
                          }
                          if (deepLink!!.getQueryParameter("podcast_id") != null) {
                              podcast_id = deepLink.getQueryParameter("podcast_id")!!
                          }
                          if (deepLink!!.getQueryParameter("filename") != null && deepLink.getQueryParameter("matchType") != null) {
                              filename = deepLink.getQueryParameter("filename")!!
                              matchType = deepLink.getQueryParameter("matchType")!!
                          }
                          if (deepLink.getQueryParameter("matchesMode") != null) {
                               matchesMode = deepLink.getQueryParameter("matchesMode")!!
                          }
                      } catch (ex:Exception){}
                      fetchDynamicData.onFetchSuccess()
                  }else{
                      fetchDynamicData.onFetchSuccess()
                  }
              }catch (e:Exception){
                  e.printStackTrace()
                  fetchDynamicData.onFetchSuccess()
              }
           }
           .addOnFailureListener(this) {
               e -> Log.w("TAG", "getDynamicLink:onFailure", e)
           }
   ```
   ***Java***
   ```java
       FirebaseDynamicLinks.getInstance().getDynamicLink(intent)
               .addOnSuccessListener(new OnSuccessListener<PendingDynamicLinkData>() {
                   @Override
                   public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                       Uri deepLink = null;
                       if (pendingDynamicLinkData != null) {
                           deepLink = pendingDynamicLinkData.getLink();
                           if(deepLink.getQueryParameter("feed_id") != null) {
                               post_id = deepLink.getQueryParameter("feed_id");
                           }
                           if (deepLink.getQueryParameter("filename") != null && deepLink.getQueryParameter("matchType") != null) {
                               filename = deepLink.getQueryParameter("filename");
                               matchType = deepLink.getQueryParameter("matchType");
                           }
                           if (deepLink.getQueryParameter("podcast_id") != null) {
                               podcast_id = deepLink.getQueryParameter("podcast_id");
                           }
                           if (deepLink.getQueryParameter("matchesMode") != null) {
                                matchesMode = deepLink.getQueryParameter("matchesMode");
                           }
                       }
                   }
               })
               .addOnFailureListener(new OnFailureListener() {
                   @Override
                   public void onFailure(@NonNull Exception e) {
                       e.printStackTrace();
                   }
               });
   ```
2) Add the fetched params as extras to intent so that you can pass that as param in FeedSdk.handleIntent(this, intent)**[Step 2 in Handle Notifications]**

   ***Kotlin***
   ```kotlin
       if(post_id.isNotEmpty()) intent.putExtra("post_id", post_id)
       if(podcast_id.isNotEmpty()) intent.putExtra("podcast_id",podcast_id)
       if(filename.isNotEmpty() && matchType.isNotEmpty()){
           intent.putExtra("filename", filename)
           intent.putExtra("matchType", matchType)
       }
       if(matchesMode.isNotEmpty()) intent.putExtra("matchesMode", matchesMode)
       // ...
       //pass the intent as param to FeedSdk.handleIntent(this, intent)[Step 2 in Handle Notifications]
       // ...
       else if (intent.extras != null && !FeedSdk.handleIntent(this, intent)) {
            //Handle your parent app notifications
       }
       // ...
       
   ```
   ***Java***
   ```java
       if (!post_id.isEmpty())
           intent.putExtra("post_id", post_id);
       if (!filename.isEmpty() && !matchType.isEmpty()) {
           intent.putExtra("filename", filename);
           intent.putExtra("matchType", matchType);
       }
       if (!podcast_id.isEmpty())
           intent.putExtra("podcast_id", podcast_id);
       if(!matchesMode.isEmpty())
           intent.putExtra("matchesMode", matchesMode);
       // ...
       //pass the intent as param to FeedSdk.handleIntent(this, intent)[Step 2 in Handle Notifications]
       // ...
       else if (getIntent().getExtras() != null && !FeedSdk.Companion.handleIntent(this, getIntent())) {
            //Handle your parent app notifications
       }
       // ...
   ```

## Notification Payload Examples

[(Back to top)](#table-of-contents)

### Payload to open feed detail Page

```groovy
{
    "to" : "/topics/Debug",
    "collapse_key" : "type_a",
    "data": {
        "title": "15 Photos Showing Kids’ Reactions to Stuff From the 1990s",
        "message": "15 Photos Showing Kids’ Reactions to Stuff From the 1990s",
        "image": "https://s3.amazonaws.com/asviral-wp-media/wp-content/uploads/2021/05/25193613/14-3-1.jpg",
        "link": "https://asviral.com/15-photos-showing-kids-reaction-to-stuff-from-the-1990s/?utm_source=push_Messenger Pro&utm_medium=asgrowth",
        "which": "L",
        "push_source": "feedsdk",
        "feed_type": "push",
        "post_id": "news_indiaheraldnews_9a2e4f2321ed47c0d7feea767bbd1892",
    }
}
```

### Payload to open post as first card in feed category

```groovy
{
    "to" : "/topics/Debug",
    "collapse_key" : "type_a",
    "data": {
        "title": "15 Photos Showing Kids’ Reactions to Stuff From the 1990s",
        "message": "15 Photos Showing Kids’ Reactions to Stuff From the 1990s",
        "image": "https://s3.amazonaws.com/asviral-wp-media/wp-content/uploads/2021/05/25193613/14-3-1.jpg",
        "link": "https://asviral.com/15-photos-showing-kids-reaction-to-stuff-from-the-1990s/?utm_source=push_Messenger Pro&utm_medium=asgrowth",
        "which": "L",
        "push_source": "feedsdk",
        "post_source": "trending",
        "interests":"news",
        "feed_type": "push",
        "post_id": "news_indiaheraldnews_9a2e4f2321ed47c0d7feea767bbd1892",
        "short_video":false
    }
}
```
1) "interests" can be changed to your required category tab to open in feeds.
2) If you open the short video post in reels then change "short_video" to **true** and add respective "post_id".

### Payload to open cricket match detail page.

```groovy
{
    "to" : "/topics/Debug",
    "collapse_key" : "type_a",
    "data": {
        "title": "PBKS Gets 1st Wicket, Rohit Sharma Leaves!",
        "message": "Ravi Bishnoi Gives PBKS Breakthrough. Catch LIVE Action",
        "image": "https://cricketimage.blob.core.windows.net/notifications/1st-Wicket_2021-09-28T21:45:02.616851+05:30.png",
        "filename": "mikp09282021204181",
        "which": "L",
        "launchType": "cricket",
        "post_source": "ipl_push"
    }
}
```

### Payload to open podcasat detail page.

```groovy
{
 "to" : "/topics/Debug",
 "collapse_key" : "type_a",
 "data": {
        "title": "15 Photos Showing Kids’ Reactions to Stuff From the 1990s",
	    "message": "15 Photos Showing Kids’ Reactions to Stuff From the 1990s",
	    "image": "https://s3.amazonaws.com/asviral-wp-media/wp-content/uploads/2021/05/25193613/14-3-1.jpg",
	    "link": "https://asviral.com/15-photos-showing-kids-reaction-to-stuff-from-the-1990s/?utm_source=push_Messenger Pro&utm_medium=asgrowth",
	    "which": "L",
	    "push_source": "feedsdk",
	    "post_source": "trending",
	    "post_id": "podcast_podcastIndex_637249_165e4c88900c19c079657b29c3f9a070",
	    "page":"SDK://podcastDetail"
    }
}