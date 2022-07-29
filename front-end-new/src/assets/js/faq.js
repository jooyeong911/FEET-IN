/* eslint-disable */
function faq() {

    // * scroll
    const sr = ScrollReveal({
        origin: 'top',
        distance: '60px',
        duration: 1500,
        delay: 20
    })

    sr.reveal(`.section-title`)
    sr.reveal(`.faq-container`, {delay: 100, distance: '20px', origin: 'bottom'})

    // * toggle content
    $(document).ready( function() {
        $( '.view' ).click( function() {
            $(this).parent().parent().next( '.detail' ).toggle();
        });
    });
}

export default faq;
