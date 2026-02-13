
TODO: check for update in repo https://klibs.io/project/dshatz/pdfmp

@Composable

fun OpenPdfInNewTabButton(url: String, modifier: Modifier = Modifier) {

    val state by rememberPdfViewerState(url)

    PdfViewer(pdfViewerState = state) // NOT_Implemented_for_web

}