import {ReactNode} from "react";

interface ModalProperties {
    open: boolean
    title: ReactNode
    body: ReactNode
    actions: ReactNode
}

function Modal({open = false, title, body, actions}: ModalProperties) {
    return <div className="relative z-10" aria-labelledby="modal-title" role="dialog" aria-modal="true" style={{display: open ? "initial" : "none"}}>
        <div
            className={`fixed inset-0 bg-gray-500 ${open ? "bg-opacity-75" : "bg-opacity-0"} transition-opacity`}></div>

        <div
            className={`fixed inset-0 z-10 w-screen overflow-y-auto ${open ? "opacity-100 translate-y-0 sm:scale-100" : "opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"}`}>
            <div className="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
                <div
                    className="relative transform overflow-hidden rounded-lg bg-white text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg">
                    <div className="bg-white px-4 pb-4 pt-5 sm:p-6 sm:pb-4">
                        <div className="sm:flex sm:items-start">
                            <div className="mt-3 text-center sm:ml-4 sm:mt-0 sm:text-left">
                                <h3 className="text-base font-semibold leading-6 text-gray-900"
                                    id="modal-title">{title}</h3>
                                <div className="mt-2">
                                    {body}
                                    {/*<p className="text-sm text-gray-500"></p>*/}
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6">
                        {actions}
                    </div>
                </div>
            </div>
        </div>
    </div>
}

export default Modal