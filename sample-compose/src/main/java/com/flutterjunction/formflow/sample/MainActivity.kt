package com.flutterjunction.formflow.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.flutterjunction.formflow.compose.collectState
import com.flutterjunction.formflow.compose.displayError
import com.flutterjunction.formflow.compose.formFieldFocus
import com.flutterjunction.formflow.compose.onChange
import com.flutterjunction.formflow.compose.rememberFormController
import com.flutterjunction.formflow.core.FieldKey
import com.flutterjunction.formflow.core.FieldState
import com.flutterjunction.formflow.core.FormStatus
import com.flutterjunction.formflow.core.RemoteError
import com.flutterjunction.formflow.core.SubmitPolicy
import com.flutterjunction.formflow.core.SubmitResult
import com.flutterjunction.formflow.core.ValidationError
import com.flutterjunction.formflow.core.Validator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val submitCount = AtomicInteger(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface {
                    LoginSample(
                        scope = scope,
                        submitCount = submitCount
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginSample(
    scope: CoroutineScope,
    submitCount: AtomicInteger
) {

    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    val form = rememberFormController(scope)

    val email = remember {
        form.registerField(
            key = FieldKey("email"),
            initial = "",
            validators = listOf(
                Validator<String> { v, _ ->
                    if (v.isBlank()) listOf(ValidationError("Email required")) else emptyList()
                }
            )
        )
    }

    val password = remember {
        form.registerField(
            key = FieldKey("password"),
            initial = "",
            validators = listOf(
                Validator<String> { v, _ ->
                    if (v.length < 6) listOf(ValidationError("Min 6 characters")) else emptyList()
                }
            )
        )
    }
    fun submitForm() {

        scope.launch {
            // Clear banner before new submit


            form.applyRemoteErrors(emptyList())

            val result = form.submit(policy = SubmitPolicy.QueueLatestBroadcast) { snap ->
                // Fake network delay
                delay(400)

                val emailValue = snap.get<String>(FieldKey("email"))

                // Fake backend:
                // first submit -> server field error on email
                // second submit -> success
                if (submitCount.incrementAndGet() == 1) {
                    SubmitResult.Rejected(
                        listOf(RemoteError.Field(FieldKey("email"), "Email already exists"))
                    )
                } else {
                    SubmitResult.Ok("Welcome $emailValue")
                }
            }

            when (result) {
                is SubmitResult.Ok -> {
                    form.applyRemoteErrors(listOf(RemoteError.Global("✅ ${result.data}")))
                }

                is SubmitResult.Rejected -> {
                    // errors already applied by controller in your submit() impl
                    // optionally show a global banner too
                    form.applyRemoteErrors(
                        listOf(RemoteError.Global("❌ Fix errors and try again"))
                    )
                }
            }
        }
    }
    val emailState by email.collectState()
    val passwordState by password.collectState()
    val formState by form.state.collectAsState()

    val submitting = formState.status is FormStatus.Submitting
    val banner = formState.globalErrors.firstOrNull()?.message

    fun shouldShowError(
        field: FieldState<*>,
        formSubmitted: Boolean
    ): Boolean {
        return field.touched || formSubmitted
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("FormFlow Sample (Login)", style = MaterialTheme.typography.titleLarge)

        if (banner != null) {
            Text(
                text = banner,
                style = MaterialTheme.typography.bodyMedium
            )
        }



        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocus)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        email.focus()
                    } else {
                        email.blur()
                    }
                },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    passwordFocus.requestFocus()
                }
            ),
            value = emailState.value,
            onValueChange = email.onChange(),
            label = { Text("Email") },

            isError = shouldShowError(emailState, formState.submittedOnce)
                    && emailState.hasError,
            supportingText = {
                if (shouldShowError(emailState, formState.submittedOnce)) {
                    emailState.displayError()?.let { Text(it) }
                }
            }

        )


        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocus)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        password.focus()
                    } else {
                        password.blur()
                    }
                },
            value = passwordState.value,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardActions = KeyboardActions(
                onDone = {
                    scope.launch {
                        submitForm()
                    }
                }
            ),
            onValueChange = password.onChange(),
            label = { Text("Password") },
            isError = shouldShowError(passwordState, formState.submittedOnce)
                    && passwordState.hasError,
            supportingText = {
                if (shouldShowError(passwordState, formState.submittedOnce)) {
                    passwordState.displayError()?.let { Text(it) }
                }
            }

        )


        Spacer(Modifier.height(8.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !submitting,
            onClick = {
                submitForm()
            }
        ) {
            Text(if (submitting) "Submitting..." else "Submit")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !submitting,
            onClick = {
                // Reset demo state
                submitCount.set(0)
                email.reset()
                password.reset()
                form.clearRemoteErrors()
            }
        ) {
            Text("Reset")
        }
    }
}



