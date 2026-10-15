package br.com.bussola.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
public record DefinirPesosRequest(@NotNull @Size(min = 5, max = 20) List<@NotNull @Valid Item> criterios) {
    public record Item(@NotNull Long id, @NotNull @Min(1) @Max(5) Integer peso) {}
}
