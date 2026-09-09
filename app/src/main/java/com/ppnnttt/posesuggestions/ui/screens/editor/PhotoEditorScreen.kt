package com.ppnnttt.posesuggestions.ui.screens.editor

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.ppnnttt.posesuggestions.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private enum class Tool { FILTER, ADJUST, TRANSFORM, AI }
private enum class Preset(val title:String,val b:Float,val c:Float,val s:Float,val w:Float){
    ORIGINAL("Original",0f,1f,1f,0f), POP("Pop",4f,1.18f,1.25f,2f), WARM("Warm",5f,1.05f,1.08f,24f),
    COOL("Cool",2f,1.08f,.95f,-24f), MONO("Mono",4f,1.14f,0f,0f), FADE("Fade",12f,.86f,.82f,6f), CINEMA("Cinema",-5f,1.22f,.72f,-5f)
}
private enum class Ratio(val title:String,val value:Float?){ ORIGINAL("Original",null), SQUARE("1:1",1f), PORTRAIT("4:5",.8f), STORY("9:16",.5625f), WIDE("16:9",1.7778f) }
private data class Edit(val b:Float=0f,val c:Float=1f,val s:Float=1f,val w:Float=0f,val rotation:Int=0,val flipH:Boolean=false,val flipV:Boolean=false,val preset:Preset=Preset.ORIGINAL,val ratio:Ratio=Ratio.ORIGINAL)

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PhotoEditorScreen(onNavigateBack:()->Unit){
    val context= LocalContext.current; val scope= rememberCoroutineScope(); val snack= remember{SnackbarHostState()}
    var image by remember{ mutableStateOf<Bitmap?>(null)}; var edit by remember{ mutableStateOf(Edit())}; var tool by remember{ mutableStateOf(Tool.FILTER)}
    var undo by remember{ mutableStateOf(listOf<Edit>())}; var redo by remember{ mutableStateOf(listOf<Edit>())}; var before by remember{ mutableStateOf(false)}; var busy by remember{ mutableStateOf(false)}
    fun commit(next:Edit){ if(next!=edit){undo=(undo+edit).takeLast(30);redo=emptyList();edit=next} }
    val error=stringResource(R.string.photo_save_failed); val saved=stringResource(R.string.photo_saved)
    val picker= rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)scope.launch{busy=true;image=withContext(Dispatchers.IO){load(context,uri)};edit=Edit();undo=emptyList();redo=emptyList();busy=false;if(image==null)snack.showSnackbar(error)}}
    val saver= rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")){uri->val src=image;if(uri!=null&&src!=null)scope.launch{busy=true;val out=withContext(Dispatchers.Default){render(src,edit)};val ok=withContext(Dispatchers.IO){save(context,uri,out)};out.recycle();busy=false;snack.showSnackbar(if(ok)saved else error)}}
    val segmenter= remember{SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundBitmap().build())};DisposableEffect(segmenter){onDispose{segmenter.close()}}
    Scaffold(containerColor=Color(0xFF080A0D),snackbarHost={SnackbarHost(snack)},topBar={TopAppBar(title={Text(stringResource(R.string.photo_editor))},navigationIcon={TextButton(onClick=onNavigateBack){Text(stringResource(R.string.back))}},actions={if(image!=null){TextButton(enabled=undo.isNotEmpty(),onClick={redo=redo+edit;edit=undo.last();undo=undo.dropLast(1)}){Text("Undo")};TextButton(enabled=redo.isNotEmpty(),onClick={undo=undo+edit;edit=redo.last();redo=redo.dropLast(1)}){Text("Redo")}}})}){pad->
      Column(Modifier.fillMaxSize().padding(pad)){
        Box(Modifier.fillMaxWidth().weight(1f),Alignment.Center){val src=image;if(src==null)Button({picker.launch("image/*")}){Text(stringResource(R.string.choose_photo))}else{AndroidView(factory={ImageView(it).apply{scaleType=ImageView.ScaleType.FIT_CENTER}},modifier=Modifier.fillMaxSize(),update={v->v.setImageBitmap(src);v.rotation=if(before)0f else edit.rotation.toFloat();v.scaleX=if(!before&&edit.flipH)-1f else 1f;v.scaleY=if(!before&&edit.flipV)-1f else 1f;v.colorFilter=if(before)null else ColorMatrixColorFilter(matrix(edit))});TextButton({before=!before},Modifier.align(Alignment.TopEnd)){Text(if(before)"Edited" else "Before")};if(busy)CircularProgressIndicator()}}
        if(image!=null){Row(Modifier.horizontalScroll(rememberScrollState()).padding(8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){Tool.entries.forEach{FilterChip(tool==it,{tool=it},{Text(it.name)})}}
          Column(Modifier.padding(horizontal=12.dp)){when(tool){
            Tool.FILTER->Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){Preset.entries.forEach{FilterChip(edit.preset==it,{commit(edit.copy(preset=it))},{Text(it.title)})}}
            Tool.ADJUST->{SliderRow("Brightness",edit.b,-100f..100f){commit(edit.copy(b=it))};SliderRow("Contrast",edit.c,.5f..1.6f){commit(edit.copy(c=it))};SliderRow("Saturation",edit.s,0f..2f){commit(edit.copy(s=it))};SliderRow("Warmth",edit.w,-50f..50f){commit(edit.copy(w=it))}}
            Tool.TRANSFORM->{Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){Ratio.entries.forEach{FilterChip(edit.ratio==it,{commit(edit.copy(ratio=it))},{Text(it.title)})}};Row{OutlinedButton({commit(edit.copy(rotation=(edit.rotation+90)%360))}){Text("Rotate")};OutlinedButton({commit(edit.copy(flipH=!edit.flipH))}){Text("Flip H")};OutlinedButton({commit(edit.copy(flipV=!edit.flipV))}){Text("Flip V")}}}
            Tool.AI->Button(enabled=!busy,onClick={busy=true;segmenter.process(InputImage.fromBitmap(image!!,0)).addOnSuccessListener{r->r.foregroundBitmap?.let{image=it.copy(Bitmap.Config.ARGB_8888,false);edit=Edit();undo=emptyList()};busy=false}.addOnFailureListener{busy=false;scope.launch{snack.showSnackbar(error)}}},modifier=Modifier.fillMaxWidth()){Text(stringResource(R.string.remove_background))}
          };Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({picker.launch("image/*")},Modifier.weight(1f)){Text("Replace")};OutlinedButton({commit(Edit())},Modifier.weight(1f)){Text(stringResource(R.string.reset))};Button({saver.launch("Pose_${SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date())}.png")},Modifier.weight(1f)){Text(stringResource(R.string.save))}}}
        }
      }
    }
}
@Composable private fun SliderRow(label:String,value:Float,range:ClosedFloatingPointRange<Float>,set:(Float)->Unit){Column{Text("$label  ${"%.1f".format(value)}");Slider(value,set,valueRange=range)}}
private fun matrix(e:Edit):ColorMatrix{val p=e.preset;val c=e.c*p.c;val b=e.b+p.b+(1-c)*128;val w=e.w+p.w;return ColorMatrix().apply{setSaturation(e.s*p.s);postConcat(ColorMatrix(floatArrayOf(c*(1+w/180),0f,0f,0f,b,0f,c,0f,0f,b,0f,0f,c*(1-w/180),0f,b,0f,0f,0f,1f,0f)))}}
private fun load(c:Context,u:Uri)=runCatching{val b=if(Build.VERSION.SDK_INT>=28)ImageDecoder.decodeBitmap(ImageDecoder.createSource(c.contentResolver,u)){d,_,_->d.allocator=ImageDecoder.ALLOCATOR_SOFTWARE}else{@Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(c.contentResolver,u)};b.copy(Bitmap.Config.ARGB_8888,false)}.getOrNull()
private fun render(src:Bitmap,e:Edit):Bitmap{val r=e.ratio.value;val crop=if(r==null)src else{val current=src.width.toFloat()/src.height;val w=if(current>r)(src.height*r).toInt()else src.width;val h=if(current>r)src.height else(src.width/r).toInt();Bitmap.createBitmap(src,(src.width-w)/2,(src.height-h)/2,w,h)};val filtered=Bitmap.createBitmap(crop.width,crop.height,Bitmap.Config.ARGB_8888);Canvas(filtered).drawBitmap(crop,0f,0f,Paint(1).apply{colorFilter=ColorMatrixColorFilter(matrix(e))});if(crop!==src)crop.recycle();if(e.rotation==0&&!e.flipH&&!e.flipV)return filtered;val m=Matrix().apply{postScale(if(e.flipH)-1f else 1f,if(e.flipV)-1f else 1f);postRotate(e.rotation.toFloat())};val out=Bitmap.createBitmap(filtered,0,0,filtered.width,filtered.height,m,true);filtered.recycle();return out}
private fun save(c:Context,u:Uri,b:Bitmap)=runCatching{c.contentResolver.openOutputStream(u,"w")?.use{b.compress(Bitmap.CompressFormat.PNG,100,it)}?:false}.getOrDefault(false)
